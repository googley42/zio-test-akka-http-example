package endpoint

import endpoint.model.Model
import zio.{Ref, ZIO, ZLayer}
import zio.console.Console

object InMemoryRepository {

  def inMemory(
    refMap: Ref[Map[String, Model]],
    refDb: Ref[Vector[Model]],
    refFailOnIdList: Ref[Vector[Int]]
  ) = ZLayer.fromService[Console.Service, Repository.Service] { console =>
    new Repository.Service {
      override def put(model: Model): ZIO[Any, Throwable, Unit] =
        console.putStrLn(s"putting record $model") *> //TODO: use logging
          refMap.update(map => map + (model.id -> model))

      override def getAll: ZIO[Any, Throwable, Seq[Model]] = refMap.get.map(_.values.toSeq)

      override def putFailOnIds(ids: Seq[Int]): ZIO[Any, Throwable, Unit] =
        refFailOnIdList.update(vector => vector ++ ids).unit

      override def delete(id: String): ZIO[Any, Throwable, Unit] =
        console.putStrLn(s"deleting record $id") *> refMap.update(_ - id)

      override def getFailOnIds: ZIO[Any, Throwable, Seq[Int]] = refFailOnIdList.get

    }
  }

}
