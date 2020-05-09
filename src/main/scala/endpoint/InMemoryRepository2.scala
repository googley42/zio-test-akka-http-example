package endpoint

import endpoint.model.Model
import zio.console.Console
import zio.{Ref, ZIO, ZLayer}

object InMemoryRepository2 {

  def inMemory(
    refDb: Ref[Map[String, Model]]
  ) = ZLayer.fromService[Console.Service, Repository2.Service] { console =>
    new Repository2.Service {
      override def put(model: Model): ZIO[Any, Throwable, Unit] =
        console.putStrLn(s"putting record $model") *> //TODO: use logging
          refDb.update(map => map + (model.id -> model))

      override def getAll: ZIO[Any, Throwable, Seq[Model]] = refDb.get.map(_.values.toSeq)

      override def get(id: String): ZIO[Any, Throwable, Option[Model]] = refDb.get.map(_.get(id))

      override def delete(id: String): ZIO[Any, Throwable, Unit] =
        console.putStrLn(s"deleting record $id") *> refDb.update(_ - id)
    }
  }

}
