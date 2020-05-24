package endpoint

import java.util.UUID

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import endpoint.model.{Id, Model}
import io.circe.Printer
import zio.{Runtime, ZIO}
import zio.console._
import zio.logging.{log, LogAnnotation}

class Api(r: Runtime[AppEnv]) extends ZioSupport(r) {
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  lazy val routes: Route = putAndGetAndDeleteModel

  private val putAndGetAndDeleteModel = pathPrefix("models") {
    put {
      entity(as[Model]) { model =>
        val putM: ZIO[Repository, Throwable, Unit] =
          for {
            maybeExistsAlready <- ZIO.accessM[Repository](_.get.get(model.id))
            //            _ <- ZIO.when(maybeExistsAlready.isDefined)(putStrLn(s"replacing model $maybeExistsAlready"))
            // TODO - contextual logging for model.id
            _ <- ZIO.accessM[Repository](_.get.put(model))
          } yield ()

        putM
          .orElseFail(StatusCodes.InternalServerError)
          .fold(failureStatus => complete(failureStatus), _ => complete(s"PUT $model"))
      }
    } ~ pathPrefix(Segment) { implicit id =>
      get {
        pathEnd {
          log.locally(AppLogging.customLogAnnotation(Some(id))) {
            getModel(Id(id))
              .fold(failureStatus => complete(failureStatus), model => complete(model))
          }
        }
      } ~ delete {
        pathEnd {
          log.locally(AppLogging.customLogAnnotation(Some(id))) {
            deleteModel(Id(id)).fold(failureStatus => complete(failureStatus), _ => complete(()))
          }
        }
      }
    } ~ get {
      val recordsM: ZIO[Repository, Throwable, Seq[Model]] = for {
        records <- ZIO.accessM[Repository](_.get.getAll)
      } yield records

      recordsM.fold(failureStatus => complete(failureStatus), records => complete(records))
    }
  }

  private def getModel(id: Id): ZIO[Repository, StatusCode, Model] =
    for {
      repo <- ZIO.access[Repository](_.get)
      findResult <- repo
        .get(id)
        .orElseFail(StatusCodes.InternalServerError)
        .someOrFail(StatusCodes.NotFound)
    } yield findResult

  private def deleteModel(id: Id): ZIO[Repository, Throwable, Unit] =
    for {
      _ <- ZIO.accessM[Repository](_.get.delete(id))
    } yield ()

}
