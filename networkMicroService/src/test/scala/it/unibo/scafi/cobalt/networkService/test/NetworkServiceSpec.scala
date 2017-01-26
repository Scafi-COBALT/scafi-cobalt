package it.unibo.scafi.cobalt.networkService.test

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.unibo.scafi.cobalt.networkService.core.{NetworkServiceComponent, NetworkServiceRepositoryMockComponent}
import it.unibo.scafi.cobalt.networkService.impl.AkkaHttpNetworkRoutingComponent
import org.scalatest.{Matchers, WordSpec}

class NetworkServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with SprayJsonSupport{
  import spray.json.DefaultJsonProtocol._

  "The Network service" should {
    val routing = new AkkaHttpNetworkRoutingComponent with NetworkServiceComponent with NetworkServiceRepositoryMockComponent()

    "Return a list of devices" in {
      Get("/nbrs/1") ~> routing.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Set[String]].head shouldBe "3"
      }
    }

    "Put a Device" in {
      Put("/nbrs/1/4") ~> routing.routes ~> check {
        status shouldEqual StatusCodes.Created
        //responseAs[Set[String]].head shouldBe "2"
      }
    }
  }
}