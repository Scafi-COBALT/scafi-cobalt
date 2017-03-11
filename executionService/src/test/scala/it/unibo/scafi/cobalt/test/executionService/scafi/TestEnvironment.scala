package it.unibo.scafi.cobalt.test.executionService.scafi

import it.unibo.scafi.cobalt.common.infrastructure.ExecutionContextProvider
import it.unibo.scafi.cobalt.executionService.impl.scafi.{ScafiExecutionServiceComponent, ScafiMockExecutionGatewayComponent, ScafiMockExecutionRepositoryComponent}

import scala.concurrent.ExecutionContext

/**
  * Created by tfarneti.
  */
class TestEnvironment(@transient implicit val impExecutionContext: ExecutionContext ) extends
  ScafiExecutionServiceComponent
  with ScafiMockExecutionRepositoryComponent
  with ScafiMockExecutionGatewayComponent
  with ExecutionContextProvider
  with Serializable
