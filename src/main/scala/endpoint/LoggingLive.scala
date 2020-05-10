package endpoint

import zio.logging.LogAnnotation
import zio.logging.slf4j.Slf4jLogger

object LoggingLive {
  private val logFormat = "[correlation-id = %s] %s"

  val layer = Slf4jLogger.make { (context, message) =>
    val correlationId = LogAnnotation.CorrelationId.render(
      context.get(LogAnnotation.CorrelationId)
    )
    logFormat.format(correlationId, message)
  }
}
