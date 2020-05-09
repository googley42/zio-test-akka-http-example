package endpoint

import endpoint.model.Model
import zio.ZIO

object Repository {

  trait Service {
    def put(testMsg: Model): ZIO[Any, Throwable, Unit]
    def getAll: ZIO[Any, Throwable, Seq[Model]]
    def delete(id: String): ZIO[Any, Throwable, Unit]
    def get(id: String): ZIO[Any, Throwable, Seq[Int]]
  }
}
