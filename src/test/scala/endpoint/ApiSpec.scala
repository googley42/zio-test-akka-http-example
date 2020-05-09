package endpoint

import akka.http.scaladsl.model.StatusCodes
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import endpoint.model.Model
import zio.console.Console
import zio.test.Assertion._
import zio.test.akkahttp.DefaultAkkaRunnableSpec
import zio.test.mock.Expectation._
import zio.test._
import zio.{Ref, Runtime, ULayer, ZIO}

object ApiSpec extends DefaultAkkaRunnableSpec {
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  override def spec =
    suite("ApiSpec")(
      testM("get should return OK using assertM") {

        val result = for {
          refMap <- Ref.make[Map[String, Model]](Map.empty + ("1" -> Model("1")))
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
          refMap <- Ref.make[Map[String, Model]](Map.empty + ("1" -> Model("1")))
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
          refMap <- Ref.make[Map[String, Model]](Map.empty)
          layer = Console.live >>> InMemoryRepository.inMemory(refMap)
          api = new Api(Runtime.unsafeFromLayer(layer))
          result <- Put("/put", Model("1")) ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using mocked repo") {
        val x: ULayer[Repository] = (RepositoryMock.Get(equalTo("todo")) returns value(Some(Model("todo")))) andThen
          (RepositoryMock.Put(equalTo(Model("1"))) returns unit)
        for {
          _ <- ZIO.unit //TODO
          api = new Api(Runtime.unsafeFromLayer(x))
          result <- Put("/put", Model("1")) ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using mocked repo, playing with assertions") {
        def any[T]: Assertion[T] =
          Assertion.assertion("any")()(_ => true)

        val x: ULayer[Repository] = (RepositoryMock.Get(equalTo("todo")) returns value(Some(Model("todo")))) andThen
          (RepositoryMock.Put(any[Model]) returns unit)
        for {
          _ <- ZIO.unit
          api = new Api(Runtime.unsafeFromLayer(x))
          result <- Put("/put", Model("1")) ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      }
    )
}
