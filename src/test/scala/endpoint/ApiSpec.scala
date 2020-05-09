package endpoint

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import endpoint.Main.layer
import endpoint.model.Model
import zio.console.Console
import zio.{IO, Ref, Runtime, UIO, ULayer, URIO, ZIO}
import zio.test.Assertion._
import zio.test.akkahttp.{DefaultAkkaRunnableSpec, RouteTest, RouteTestResult}
import zio.test.{assert, assertM, suite, testM, Assertion}
import RepositoryMock._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import zio.test.mock.Expectation._

object ApiSpec extends DefaultAkkaRunnableSpec {
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  override def spec =
    suite("ApiSpec")(
      testM("get should return OK") {

        val result = for {
          refRecords <- Ref.make[Vector[Model]](Vector.empty)
          refFailOnIdList <- Ref.make[Vector[Int]](Vector.empty)
          layer = Console.live >>> InMemoryRepository.inMemory(refRecords, refFailOnIdList)
          api = new Api(Runtime.unsafeFromLayer(layer))
          result <- Get("/get") ~> api.routes
        } yield result

        assertM(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("get should return OK") {
        for {
          refRecords <- Ref.make[Vector[Model]](Vector.empty)
          refFailOnIdList <- Ref.make[Vector[Int]](Vector.empty)
          layer = Console.live >>> InMemoryRepository.inMemory(refRecords, refFailOnIdList)
          api = new Api(Runtime.unsafeFromLayer(layer))
          result <- Get("/get") ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("get should return OK with assertM") {
        for {
          refRecords <- Ref.make[Vector[Model]](Vector.empty)
          refFailOnIdList <- Ref.make[Vector[Int]](Vector.empty)
          layer = Console.live >>> InMemoryRepository.inMemory(refRecords, refFailOnIdList)
          api = new Api(Runtime.unsafeFromLayer(layer))
          result <- Get("/get") ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using in memory repo") {
//        val x: IO[Nothing, Unit] = IO.unit
//        val repo = RepositoryMock.Put(equalTo(TestMsg(1))) returns unit
        for {
          refRecords <- Ref.make[Vector[Model]](Vector.empty)
          refFailOnIdList <- Ref.make[Vector[Int]](Vector.empty)
//          api = new Api(Runtime.unsafeFromLayer(repo))
          layer = Console.live >>> InMemoryRepository.inMemory(refRecords, refFailOnIdList)
          api = new Api(Runtime.unsafeFromLayer(layer))
          result <- Put("/put", Model("1")) ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using mocked repo") {
        val x: ULayer[Repository] = (RepositoryMock.GetFailOnIds returns value(Vector.empty[Int])) andThen
          (RepositoryMock.Put(equalTo(Model("1"))) returns unit)
        for {
          refRecords <- Ref.make[Vector[Model]](Vector.empty)
          refFailOnIdList <- Ref.make[Vector[Int]](Vector.empty)
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

        val x: ULayer[Repository] = (RepositoryMock.GetFailOnIds returns value(Vector.empty[Int])) andThen
          (RepositoryMock.Put(any[Model]) returns unit)
        for {
          refRecords <- Ref.make[Vector[Model]](Vector.empty)
          refFailOnIdList <- Ref.make[Vector[Int]](Vector.empty)
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
