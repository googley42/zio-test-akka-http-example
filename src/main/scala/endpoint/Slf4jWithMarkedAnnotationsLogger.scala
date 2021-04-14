package endpoint

import izumi.reflect.Tag
import org.slf4j.{LoggerFactory, Marker}
import zio.internal.Tracing
import zio.internal.stacktracer.Tracer
import zio.internal.stacktracer.ZTraceElement.{NoLocation, SourceLocation}
import zio.internal.stacktracer.impl.AkkaLineNumbersTracer
import zio.internal.tracing.TracingConfig
import zio.logging.LogAppender.Service
import zio.logging.{Appender, LogAnnotation, LogAppender, LogContext, LogFormat, LogLevel, Logging}
import zio.{UIO, ULayer, ZIO, ZLayer}

// TODO: remove - not used
object Slf4jWithMarkedAnnotationsLogger {

  // line numbers and source file extraction for zio stacktrace
  private val tracing = Tracing(
    Tracer.globallyCached(new AkkaLineNumbersTracer),
    TracingConfig.enabled
  )

  private def classNameForLambda(lambda: => AnyRef) =
    tracing.tracer.traceLocation(() => lambda) match {
      case SourceLocation(_, className, _, _) => Some(className)
      case NoLocation(_)                      => None
    }

  private def logger(name: String) =
    ZIO.effectTotal(
      LoggerFactory.getLogger(
        name
      )
    )

  private def withLoggerNameFromLine[A <: AnyRef: Tag]: ZLayer[Appender[A], Nothing, Appender[A]] =
    ZLayer.fromFunction(
      (appender: Appender[A]) =>
        new Service[A] {

          override def write(ctx: LogContext, msg: => A): UIO[Unit] = {
            val ctxWithName = ctx.get(LogAnnotation.Name) match {
              case Nil =>
                ctx.annotate(
                  LogAnnotation.Name,
                  classNameForLambda(msg).getOrElse("ZIO.defaultLogger") :: Nil
                )
              case _ => ctx
            }
            appender.get.write(ctxWithName, msg)
          }
        }
    )

  def layer(): ULayer[Logging] =
    LogAppender.make[Any, String](
      LogFormat.fromFunction((_, line) => line),
      (context, line) => {
        val loggerName = context(LogAnnotation.Name)
        logger(loggerName).map {
          slf4jLogger =>
            val maybeThrowable = context.get(LogAnnotation.Throwable).orNull
            def logContextMarker = logContext2Marker(context)
            context.get(LogAnnotation.Level).level match {
              case LogLevel.Off.level => ()
              case LogLevel.Debug.level =>
                slf4jLogger.debug(logContextMarker, line, maybeThrowable)
              case LogLevel.Trace.level =>
                slf4jLogger.trace(logContextMarker, line, maybeThrowable)
              case LogLevel.Info.level =>
                slf4jLogger.info(logContextMarker, line, maybeThrowable)
              case LogLevel.Warn.level =>
                slf4jLogger.warn(logContextMarker, line, maybeThrowable)
              case LogLevel.Error.level =>
                slf4jLogger.error(logContextMarker, line, maybeThrowable)
              case LogLevel.Fatal.level =>
                slf4jLogger.error(logContextMarker, line, maybeThrowable)
            }
        }
      }
    ) >>> withLoggerNameFromLine[String] >>>
      Logging.make

  private def logContext2Marker(logContext: LogContext): Marker = {
    import net.logstash.logback.marker.Markers._

    import scala.collection.JavaConverters._

    aggregate(logContext.renderContext.toList.map {
      case (logAnnotationName, logAnnotationVal) =>
        append(logAnnotationName, logAnnotationVal)
    }.asJava)
  }
}
