package endpoint

import akka.http.scaladsl.model.StatusCodes
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import endpoint.model.{Id, Model}
import zio.console.Console
import zio.test.Assertion._
import zio.test.akkahttp.DefaultAkkaRunnableSpec
import zio.test.mock.Expectation._
import zio.test._
import zio.{Ref, Runtime, ULayer, ZIO}

object ApiSpec extends DefaultAkkaRunnableSpec {
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  private val IdOne = Id("1")

  override def spec =
    suite("ApiSpec")(
      testM("get should return OK using assertM") {

        val result = for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          layer = Console.live >>> InMemoryRepository.inMemory(refMap)
          api = new Api(Runtime.unsafeFromLayer(layer))
          result <- Get("/get/1") ~> api.routes
        } yield result

        assertM(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("get should return OK using assert") {
        for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          layer = Console.live >>> InMemoryRepository.inMemory(refMap)
          api = new Api(Runtime.unsafeFromLayer(layer))
          result <- Get("/get/1") ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using in memory repo") {
        for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty)
          layer = Console.live >>> InMemoryRepository.inMemory(refMap)
          api = new Api(Runtime.unsafeFromLayer(layer))
          result <- Put("/put", Model(IdOne)) ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using mocked repo") {
        val x: ULayer[Repository] = (RepositoryMock.Get(equalTo(IdOne)) returns value(Some(Model(IdOne)))) andThen
          (RepositoryMock.Put(equalTo(Model(IdOne))) returns unit)
        for {
          _ <- ZIO.unit //TODO
          api = new Api(Runtime.unsafeFromLayer(x))
          result <- Put("/put", Model(IdOne)) ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using mocked repo, playing with assertions") {
        def any[T]: Assertion[T] =
          Assertion.assertion("any")()(_ => true)

        val x: ULayer[Repository] = (RepositoryMock.Get(equalTo(IdOne)) returns value(Some(Model(IdOne)))) andThen
          (RepositoryMock.Put(any[Model]) returns unit)
        for {
          _ <- ZIO.unit
          api = new Api(Runtime.unsafeFromLayer(x))
          result <- Put("/put", Model(IdOne)) ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      }
    )
}
