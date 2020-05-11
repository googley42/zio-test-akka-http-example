package endpoint

import zio.ZLayer
import zio.clock.Clock
import zio.console.Console
import zio.logging.{LogAnnotation, Logging}
import zio.logging.slf4j.Slf4jLogger

object LoggingLive {
  private val logFormat = "[correlation-id = %s] %s"

  val layer: ZLayer[Any, Nothing, Logging] = Slf4jLogger.make { (context, message) =>
    val correlationId = LogAnnotation.CorrelationId.render(
      context.get(LogAnnotation.CorrelationId)
    )
    logFormat.format(correlationId, message)
  }

  val testLayer: ZLayer[Console with Clock, Nothing, Logging] = Logging.console { (context, message) =>
    val correlationId = LogAnnotation.CorrelationId.render(
      context.get(LogAnnotation.CorrelationId)
    )
    logFormat.format(correlationId, message)
  }

}
