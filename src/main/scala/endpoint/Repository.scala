package endpoint

import endpoint.model.Model
import zio.ZIO

object Repository {

  trait Service {
    def put(testMsg: Model): ZIO[Any, Throwable, Unit]
    def getAll: ZIO[Any, Throwable, Seq[Model]]
    def putFailOnIds(ids: Seq[Int]): ZIO[Any, Throwable, Unit]
    def deleteFailOnIds(): ZIO[Any, Throwable, Unit]
    def getFailOnIds: ZIO[Any, Throwable, Seq[Int]]
  }
}