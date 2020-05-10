package endpoint

import endpoint.model.{Id, Model}
import zio.logging.{Logger, Logging}
import zio.{Ref, ZIO, ZLayer}

object InMemoryRepository {

  def inMemory(
    refMap: Ref[Map[Id, Model]]
  ): ZLayer[Logging, Nothing, Repository] =
    ZLayer.fromService[Logger[String], Repository.Service] { logger =>
      new Repository.Service {
        override def put(model: Model): ZIO[Any, Throwable, Unit] =
          logger.info(s"putting record $model") *>
            refMap.update(map => map + (model.id -> model))

        override def getAll: ZIO[Any, Throwable, Seq[Model]] = refMap.get.map(_.values.toSeq)

        override def delete(id: Id): ZIO[Any, Throwable, Unit] =
          logger.info(s"deleting record $id") *> refMap.update(_ - id)

        override def get(id: Id): ZIO[Any, Throwable, Option[Model]] =
          logger.info(s"getting record $id") *> refMap.get.map(_.get(id))
      }
    }

}
