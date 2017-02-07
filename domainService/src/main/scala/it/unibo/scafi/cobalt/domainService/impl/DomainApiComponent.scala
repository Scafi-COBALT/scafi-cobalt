package it.unibo.scafi.cobalt.domainService.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.client.{CollectorRegistry, Gauge}
import it.unibo.scafi.cobalt.common.ExecutionContextProvider
import it.unibo.scafi.cobalt.common.metrics.MetricsEndpoint
import it.unibo.scafi.cobalt.domainService.core.DomainServiceComponent
import spray.json.DefaultJsonProtocol

/**
  * Created by tfarneti.
  */

class DomainApiComponent extends DefaultJsonProtocol { self: DomainApiComponent.dependencies =>

  val metricsEndpoint = new MetricsEndpoint(CollectorRegistry.defaultRegistry)
  DefaultExports.initialize()

  val routes = {
    path("nbrs" / Segment) { (deviceId) =>
      get {
        complete {
          service.getNeighbors(deviceId)
        }
      }
    } ~
    path("nbrs" / Segment / Segment) { (deviceId,nbrId) =>
      put{
        complete(Created -> service.addNeighborForDevice(deviceId,nbrId))
      }
    }~
    path("nbrs" / "spatial" / Segment){ deviceId =>
      get{
        complete(service.getNbrsSpatial(deviceId))
      }
    }~metricsEndpoint.metricsRoute
  }
}

object DomainApiComponent{
  type dependencies = DomainServiceComponent with ExecutionContextProvider
}