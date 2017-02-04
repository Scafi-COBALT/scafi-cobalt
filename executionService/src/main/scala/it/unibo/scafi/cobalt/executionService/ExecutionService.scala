package it.unibo.scafi.cobalt.executionService

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import io.scalac.amqp._
import it.unibo.scafi.cobalt.common.{ActorMaterializerProvider, ActorSystemProvider, ExecutionContextProvider}
import it.unibo.scafi.cobalt.core.messages.{FieldData, SensorData}
import it.unibo.scafi.cobalt.executionService.core.CobaltBasicIncarnation
import it.unibo.scafi.cobalt.executionService.impl._
import it.unibo.scafi.cobalt.executionService.impl.gateway.DockerGatewayComponent
import it.unibo.scafi.cobalt.executionService.impl.repository.RedisExecutionRepositoryComponent
import redis.RedisClient
import it.unibo.scafi.cobalt.core.messages.JsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext


trait Environment extends AkkaHttpExecutionComponent
  with CobaltExecutionServiceComponent
  with RedisExecutionRepositoryComponent
  with DockerGatewayComponent
  with CobaltBasicIncarnation
  with DockerConfig
  with ServicesConfiguration
  with ActorSystemProvider
  with ActorMaterializerProvider
  with ExecutionContextProvider


object ExecutionService extends App with DockerConfig with AkkaHttpConfig with RedisConfiguration{
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit def executionContext = system.dispatcher
  val redis: RedisClient = RedisClient(host = redisHost, port = redisPort, password = Option(redisPassword), db = Option(redisDb))

  val env = new Environment {

    override val redisClient: RedisClient = redis
    override implicit val impmaterializer: ActorMaterializer = materializer
    override implicit def impExecutionContext: ExecutionContext = executionContext
    override implicit val impSystem: ActorSystem = system
  }

  Http().bindAndHandle(env.executionRoutes, interface, port)

  val connection = Connection(config)
  connection.queueDeclare(Queue("sensor_events.executionService.queue",durable = true)).onComplete(_=>
  connection.queueBind("sensor_events.executionService.queue","sensor_events","*.gps"))

  connection.exchangeDeclare(Exchange("field_events", Topic, durable = true))
  connection.queueDeclare(Queue("field_events.test.queue",durable = true)).onComplete(_=>
    connection.queueBind("field_events.test.queue","field_events","*"))

  Source.fromPublisher(connection.consume(queue = "sensor_events.executionService.queue"))
    .map(m => ByteString.fromArray(m.message.body.toArray).utf8String.parseJson.convertTo[SensorData])
    .mapAsync(1)(data => {
      env.service.computeNewState(data.deviceId).map(a => a -> data.sensorValue.split(":"))
    })
    .map(s => Routed( s"${s._1.id}", Message(body = ByteString(FieldData(s._1.id,s._2(0).toDouble,s._2(1).toDouble).toJson.compactPrint))))
    .runWith(Sink.fromSubscriber(connection.publish(exchange = "field_events")))
}
