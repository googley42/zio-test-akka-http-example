package endpoint

import endpoint.model.Model
import zio.{Ref, ZIO, ZLayer}
import zio.console.Console

object InMemoryRepository {

  def inMemory(
    refDb: Ref[Vector[Model]],
    refFailOnIdList: Ref[Vector[Int]]
  ) = ZLayer.fromService[Console.Service, Repository.Service] { console =>
    new Repository.Service {
      override def put(testMsg: Model): ZIO[Any, Throwable, Unit] =
        for {
          _ <- console.putStrLn(s"putting record $testMsg") //TODO: use logging
          _ <- refDb.update(vector => vector :+ testMsg)
        } yield ()

      override def getAll: ZIO[Any, Throwable, Seq[Model]] = refDb.get

      override def putFailOnIds(ids: Seq[Int]): ZIO[Any, Throwable, Unit] =
        refFailOnIdList.update(vector => vector ++ ids).unit

      override def deleteFailOnIds(): ZIO[Any, Throwable, Unit] =
        refFailOnIdList.update(_ => Vector.empty).unit

      override def getFailOnIds: ZIO[Any, Throwable, Seq[Int]] = refFailOnIdList.get

    }
  }

}
