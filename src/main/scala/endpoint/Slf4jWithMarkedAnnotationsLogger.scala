package endpoint

import org.slf4j.{LoggerFactory, Marker}
import zio.internal.Tracing
import zio.internal.stacktracer.Tracer
import zio.internal.stacktracer.ZTraceElement.{NoLocation, SourceLocation}
import zio.internal.stacktracer.impl.AkkaLineNumbersTracer
import zio.internal.tracing.TracingConfig
import zio.logging.{LogAnnotation, LogContext, LogLevel, Logging}
import zio.{ULayer, ZIO}

//object LogAnnotations {
//
//  val InvoiceId: LogAnnotation[Option[String]] =
//    LogAnnotation[Option[String]]("invoice-id", None, (_, a) => a, _.getOrElse("invoice-id-not-set"))
//
//  val all: Seq[LogAnnotation[Option[String]]] = Seq(InvoiceId)
//
//}

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

  def make(logAnnotations: LogAnnotation[_]*): ULayer[Logging] =
    Logging.make { (context, line) =>
      val loggerName = context.get(LogAnnotation.Name) match {
        case Nil   => classNameForLambda(line).getOrElse("ZIO.defaultLogger")
        case names => LogAnnotation.Name.render(names)
      }
      logger(loggerName).map(
        slf4jLogger =>
          context.get(LogAnnotation.Level).level match {
            case LogLevel.Off.level => ()
            case LogLevel.Debug.level =>
              slf4jLogger
                .debug(logContext2Marker(context, logAnnotations), line)
            case LogLevel.Trace.level =>
              slf4jLogger
                .trace(logContext2Marker(context, logAnnotations), line)
            case LogLevel.Info.level =>
              slf4jLogger
                .info(logContext2Marker(context, logAnnotations), line)
            case LogLevel.Warn.level =>
              slf4jLogger
                .warn(logContext2Marker(context, logAnnotations), line)
            case LogLevel.Error.level | LogLevel.Fatal.level =>
              slf4jLogger
                .error(logContext2Marker(context, logAnnotations), line)
          }
      )
    }

  private def logContext2Marker(logContext: LogContext, logAnnotations: Seq[LogAnnotation[_]]): Marker = {
    import net.logstash.logback.marker.Markers._
    import scala.collection.JavaConverters._
    def fromLogAnnotation[A](logAnnotation: LogAnnotation[A]): Marker =
      append(
        logAnnotation.name,
        logAnnotation.render(logContext.get(logAnnotation))
      )
    aggregate(logAnnotations.map(l => fromLogAnnotation(l)).asJava)
  }
}
