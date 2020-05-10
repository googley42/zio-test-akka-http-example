package endpoint

import endpoint.model.{Id, Model}
import zio.ZIO

object Repository {

  trait Service {
    def put(testMsg: Model): ZIO[Any, Throwable, Unit]
    def getAll: ZIO[Any, Throwable, Seq[Model]]
    def delete(id: Id): ZIO[Any, Throwable, Unit]
    def get(id: Id): ZIO[Any, Throwable, Option[Model]]
  }
}
