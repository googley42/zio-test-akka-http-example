package endpoint

import zio.ZLayer
import zio.clock.Clock
import zio.console.Console
import zio.logging.{LogAnnotation, LogFormat, LogLevel, Logging}
import zio.logging.slf4j.Slf4jLogger

object AppLogging {
  private val logFormat = "[custom_id = %s] %s"

  val customLogAnnotation = LogAnnotation[Option[String]](
    name = "custom_id",
    initialValue = None,
    combine = (_, r) => r,
    render = _.getOrElse("undefined-custom_id")
  )

  val all: Seq[LogAnnotation[Option[String]]] = Seq(customLogAnnotation)

  val layer: ZLayer[Any, Nothing, Logging] = Slf4jLogger.make { (context, message) =>
    val customId = customLogAnnotation.render(
      context.get(customLogAnnotation)
    )
    logFormat.format(customId, message)
  }

  val testLayer = Logging.console(
    logLevel = LogLevel.Info,
    format = LogFormat.SimpleConsoleLogFormat()
  ) >>> Logging.withRootLoggerName("my-component")

}
