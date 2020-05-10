package endpoint

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import endpoint.model.{Id, Model}
import io.circe.Printer
import zio.{Runtime, ZIO}
import zio.console._

class Api(r: Runtime[Repository]) extends ZioSupport(r) {
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  lazy val routes: Route = putAndGetModel ~ deleteModel

  private val putAndGetModel = pathPrefix("models") {
    put {
      entity(as[Model]) { model =>
        val putM: ZIO[Repository, Throwable, Unit] =
          for {
            maybeExistsAlready <- ZIO.accessM[Repository](_.get.get(model.id))
            //            _ <- ZIO.when(maybeExistsAlready.isDefined)(putStrLn(s"replacing model $maybeExistsAlready")) // TODO - some logging
            _ <- ZIO.accessM[Repository](_.get.put(model))
          } yield ()

        putM
          .orElseFail(StatusCodes.InternalServerError)
          .fold(failureStatus => complete(failureStatus), _ => complete(s"PUT $model"))
      }
    } ~ pathPrefix(Segment) { implicit id =>
      get {
        pathEnd {
          getModel(Id(id))
            .fold(failureStatus => complete(failureStatus), model => complete(model))
        }
      }
    } ~ get {
      val recordsM: ZIO[Repository, Throwable, Seq[Model]] = for {
        records <- ZIO.accessM[Repository](_.get.getAll)
      } yield records

      recordsM.fold(failureStatus => complete(failureStatus), records => complete(records))
    }
  }

//  private lazy val putModel: Route = put {
//    path("put") {
//      entity(as[Model]) { model =>
//        val putM: ZIO[Repository, Throwable, Unit] =
//          for {
//            maybeExistsAlready <- ZIO.accessM[Repository](_.get.get(model.id))
////            _ <- ZIO.when(maybeExistsAlready.isDefined)(putStrLn(s"replacing model $maybeExistsAlready")) // TODO - some logging
//            _ <- ZIO.accessM[Repository](_.get.put(model))
//          } yield ()
//
//        putM
//          .orElseFail(StatusCodes.InternalServerError)
//          .fold(failureStatus => complete(failureStatus), _ => complete(s"PUT $model"))
//      }
//    }
//  }

//  private lazy val getModel: Route = pathPrefix("get") {
//    get {
//      pathPrefix(Segment) { id =>
//        pathEnd {
//          getModel(Id(id))
//            .fold(failureStatus => complete(failureStatus), model => complete(model))
//        }
//      }
//    }
//  }

  private def getModel(id: Id): ZIO[Repository, StatusCode, Model] =
    for {
      repo <- ZIO.access[Repository](_.get)
      findResult <- repo
        .get(id)
        .orElseFail(StatusCodes.InternalServerError)
        .someOrFail(StatusCodes.NotFound)
    } yield findResult

  private lazy val deleteModel: Route = delete {
    val deleteM: ZIO[Repository, Throwable, Unit] = for {
      _ <- ZIO.accessM[Repository](_.get.delete(Id("todo")))
    } yield ()

    path("deleteFailOnIds") {
      deleteM.fold(failureStatus => complete(failureStatus), _ => complete(()))
    }
  }

//  private lazy val getAll: Route = get {
//    val recordsM: ZIO[Repository, Throwable, Seq[Model]] = for {
//      records <- ZIO.accessM[Repository](_.get.getAll)
//    } yield records
//
//    path("getAll") {
//      recordsM.fold(failureStatus => complete(failureStatus), records => complete(records))
//    }
//  }

}
