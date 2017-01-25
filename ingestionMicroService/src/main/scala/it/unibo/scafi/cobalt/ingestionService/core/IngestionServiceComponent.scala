package it.unibo.scafi.cobalt.ingestionService.core

import it.unibo.scafi.cobalt.core.messages.ingestionService.{UpdateSensorValueCmd, UpdateSensorsValues}

import scala.concurrent.Future

/**
  * Created by tfarneti.
  */
trait IngestionServiceComponent { self : IngestionServiceComponent.dependencies =>
  def service = new IngestionService()

  class IngestionService{
    def updateSensorValue(cmd: UpdateSensorValueCmd): Future[Boolean] = {
      repository.setSensorValue(cmd.deviceId,cmd.sensorName,cmd.sensorValue)
    }
    def updateSensorsValues(cmd: UpdateSensorsValues): Future[Boolean] = {
      repository.setSensorsValues(cmd.deviceId,cmd.sensorsValues)
    }
  }
}

object IngestionServiceComponent{
  type dependencies = IngestionServiceRepositoryComponent
}

