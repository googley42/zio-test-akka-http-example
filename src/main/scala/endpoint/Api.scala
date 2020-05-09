package endpoint

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import endpoint.model.Model
import io.circe.Printer
import zio.{Runtime, ZIO}

class Api(r: Runtime[Repository]) extends ZioSupport(r) {
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  private implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  lazy val routes: Route = putModel ~ getModel ~ deleteModel ~ getAll

  private lazy val putModel: Route = put {
    path("put") {
      entity(as[Model]) { msg =>
        val putM =
          for {
            failOnIds <- ZIO.accessM[Repository](_.get.get("todo"))
            resp <- if (failOnIds.contains(msg.id))
              ZIO.fail(new RuntimeException("BOOM!"))
            else
              ZIO.accessM[Repository](_.get.put(msg))
          } yield resp

        putM.fold(failureStatus => complete(failureStatus), _ => complete(s"PUT $msg"))
      }
    }
  }

  private lazy val getModel: Route = get {
    val recordsM: ZIO[Repository, Throwable, Option[Model]] = for {
      maybeModel <- ZIO.accessM[Repository](_.get.get("todo"))
    } yield maybeModel

    path("get") {
      recordsM.fold(failureStatus => complete(failureStatus), records => complete(records))
    }
  }

  private lazy val deleteModel: Route = delete {
    val deleteM: ZIO[Repository, Throwable, Unit] = for {
      _ <- ZIO.accessM[Repository](_.get.delete("todo"))
    } yield ()

    path("deleteFailOnIds") {
      deleteM.fold(failureStatus => complete(failureStatus), _ => complete(()))
    }
  }

  private lazy val getAll: Route = get {
    val recordsM: ZIO[Repository, Throwable, Seq[Model]] = for {
      records <- ZIO.accessM[Repository](_.get.getAll)
    } yield records

    path("getAll") {
      recordsM.fold(failureStatus => complete(failureStatus), records => complete(records))
    }
  }

}
