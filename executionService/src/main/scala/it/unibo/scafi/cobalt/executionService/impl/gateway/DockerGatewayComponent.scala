package it.unibo.scafi.cobalt.executionService.impl.gateway

import java.io.IOException

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.Success
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import it.unibo.scafi.cobalt.executionService.core.ExecutionGatewayComponent
import it.unibo.scafi.cobalt.executionService.impl.{ActorSystemProvider, ServicesConfiguration}
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

/**
  * Created by tfarneti.
  */
trait DockerGatewayComponent extends ExecutionGatewayComponent{ self: ServicesConfiguration with ActorSystemProvider =>
  override def gateway = new DockerGateway()

  class DockerGateway extends Gateway{
    override def GetAllNbrsIds(id: String): Future[Set[String]] = {
      val request = RequestBuilding.Get(s"/nbrs/spatial/$id")

      Source.single(request).via(Http().outgoingConnection(networkHost,networkPort)).runWith(Sink.head).flatMap {response =>
        response.status match {
          case Success(_) => Unmarshal(response.entity).to[Set[String]]
          case _ => Future.failed(new IOException("Epic Fail"))
        }
      }
    }

    override def GetSensors(id: String): Future[Map[String, String]] = {
      val request = RequestBuilding.Get(s"/device/$id/sensor/gps")

      Source.single(request).via(Http().outgoingConnection(sensorHost,sensorPort)).runWith(Sink.head).flatMap{response =>
        response.status match {
          case Success(_) => Unmarshal(response.entity).to[Map[String,String]]
          case _ => Future.failed(new IOException("Epic Fail"))
        }
      }
    }

  }
}