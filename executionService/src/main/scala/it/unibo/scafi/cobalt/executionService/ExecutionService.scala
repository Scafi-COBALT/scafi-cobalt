package it.unibo.scafi.cobalt.executionService

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import io.prometheus.client.Counter
import io.scalac.amqp._
import it.unibo.scafi.cobalt.common.infrastructure.{ActorMaterializerProvider, ActorSystemProvider, ExecutionContextProvider}
import it.unibo.scafi.cobalt.common.messages.JsonProtocol._
import it.unibo.scafi.cobalt.common.messages.{FieldData, SensorData}
import it.unibo.scafi.cobalt.executionService.impl._
import it.unibo.scafi.cobalt.executionService.impl.cobalt._
import redis.RedisClient
import spray.json._

import scala.concurrent.ExecutionContext


trait Environment extends ExecutionApiComponent
  with CobaltExecutionServiceComponent
  with CobaltRedisExecutionRepositoryComponent
  with CobaltExecutionGatewayComponent
  with CobaltBasicIncarnation
  with DockerConfig
  with ServicesConfiguration
  with ActorSystemProvider
  with ActorMaterializerProvider
  with ExecutionContextProvider


object ExecutionService extends App with DockerConfig with AkkaHttpConfig with RedisConfiguration with CobaltBasicIncarnation{
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

  val requestsServed = Counter.build()
    .name("requests_served_total")
      .help("Total Request processed").register()

  val connection = Connection(config)
  connection.queueDeclare(Queue("sensor_events.executionService.queue",durable = true)).onComplete(_=>
  connection.queueBind("sensor_events.executionService.queue","sensor_events","*.gps"))

  connection.exchangeDeclare(Exchange("field_events", Topic, durable = true))
  connection.queueDeclare(Queue("field_events.test.queue",durable = true)).onComplete(_=>
    connection.queueBind("field_events.test.queue","field_events","*"))

  import akka.stream.ActorAttributes.supervisionStrategy
  import akka.stream.Supervision.resumingDecider

  Source.fromPublisher(connection.consume(queue = "sensor_events.executionService.queue"))
    .map(m => ByteString.fromArray(m.message.body.toArray).utf8String.parseJson.convertTo[SensorData])
     .mapAsync(1)(data => {
      requestsServed.inc()
      env.service.execRound(data.deviceId).map(a => data.deviceId -> data.sensorValue.split(":"))
    })
    .withAttributes(supervisionStrategy(resumingDecider))
    .map(s => Routed( s"${s._1}", Message(body = ByteString(FieldData(s._1,s._2(0).toDouble,s._2(1).toDouble).toJson.compactPrint))))
    .runWith(Sink.fromSubscriber(connection.publish(exchange = "field_events")))
}