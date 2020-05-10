package endpoint

import endpoint.model.{Id, Model}
import zio.{Ref, ZIO, ZLayer}
import zio.console.Console

object InMemoryRepository {

  def inMemory(
    refMap: Ref[Map[Id, Model]]
  ) = ZLayer.fromService[Console.Service, Repository.Service] { console =>
    new Repository.Service {
      override def put(model: Model): ZIO[Any, Throwable, Unit] =
        console.putStrLn(s"putting record $model") *> //TODO: use logging
          refMap.update(map => map + (model.id -> model))

      override def getAll: ZIO[Any, Throwable, Seq[Model]] = refMap.get.map(_.values.toSeq)

      override def delete(id: Id): ZIO[Any, Throwable, Unit] =
        console.putStrLn(s"deleting record $id") *> refMap.update(_ - id)

      override def get(id: Id): ZIO[Any, Throwable, Option[Model]] = refMap.get.map(_.get(id))
    }
  }

}
