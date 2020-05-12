package endpoint

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import endpoint.model.{Id, Model}
import zio.console.Console
import zio.test.Assertion._
import zio.test.akkahttp.DefaultAkkaRunnableSpec
import zio.test.mock.Expectation._
import zio.test._
import zio.{Ref, Runtime, ULayer, ZIO}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}

object ApiSpec extends DefaultAkkaRunnableSpec {
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  private val IdOne = Id("1")
  private val IdTwo = Id("2")

  // TODO inject mock console
  private val log = (Console.live ++ zio.clock.Clock.live) >>> LoggingLive.testLayer

  private def layer(refMap: Ref[Map[Id, Model]]) =
    log ++ (log >>> InMemoryRepository.inMemory(refMap))

  override def spec =
    suite("ApiSpec")(
      testM("get all should return all records using assertM") {

        val result = for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          api = new Api(Runtime.unsafeFromLayer(layer(refMap)))
          result <- Get("/models") ~> api.routes
        } yield result

        assertM(result)(
          handled(
            status(equalTo(StatusCodes.OK))
              && entityAs[Seq[Model]](isRight(equalTo(Seq(Model(IdOne)))))
          )
        )
      },
      testM("get should return OK using assertM") {

        val result = for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          api = new Api(Runtime.unsafeFromLayer(layer(refMap)))
          result <- Get("/models/1") ~> api.routes
        } yield result

        assertM(result)(
          handled(
            status(equalTo(StatusCodes.OK))
              && entityAs[Model](isRight(equalTo(Model(IdOne))))
          )
        )
      },
      testM("delete should delete Model from repository") {
        for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          api = new Api(Runtime.unsafeFromLayer(layer(refMap)))
          assertRoute <- assertM(Delete("/models/1") ~> api.routes)(
            handled(
              status(equalTo(StatusCodes.OK))
            )
          )
          assertModel <- assertM(refMap.get)(equalTo(Map.empty[Id, Model]))
        } yield assertRoute && assertModel
      },
      testM("get should return OK using assert") {
        for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          api = new Api(Runtime.unsafeFromLayer(layer(refMap)))
          result <- Get("/models/1") ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using in memory repo") {
        for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty)
          api = new Api(Runtime.unsafeFromLayer(layer(refMap)))
          result <- Put("/models", Model(IdOne)) ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using mocked repo") {
        val mockRepo: ULayer[Repository] = (MockRepository
          .Get(equalTo(IdOne)) returns value(Some(Model(IdOne)))) andThen
          (MockRepository.Put(equalTo(Model(IdOne))) returns unit)
        for {
          _ <- ZIO.unit //TODO
          api = new Api(Runtime.unsafeFromLayer(LoggingLive.layer ++ mockRepo))
          result <- Put("/models", Model(IdOne)) ~> api.routes
        } yield assert(result)(
          handled(
            status(equalTo(StatusCodes.OK))
          )
        )
      },
      testM("put using mocked repo, playing with custom any[T] assertion") {

        def getAnyModel(model: Model) = MockRepository.Get(anything) returns value(Some(model))
        val putAnyModel = MockRepository.Put(anything) returns unit
        def mockRepo(model: Model): ULayer[Repository] =
          getAnyModel(model) andThen putAnyModel // note we need type annotation : ULayer[Repository] else we get java.lang.Error: Defect in zio.Has: Could not find Repository::Service inside Map(Logger[-String] -> zio.logging.Logging$$anon$1@18fac746)

        for {
          _ <- ZIO.unit
          api1 = new Api(Runtime.unsafeFromLayer(LoggingLive.layer ++ mockRepo(Model(IdOne))))
          assertRoute1 <- assertM(Put("/models", Model(IdOne)) ~> api1.routes)(
            handled(
              status(equalTo(StatusCodes.OK)) ?? "Model(IdOne)"
            )
          )
          api2 = new Api(Runtime.unsafeFromLayer(LoggingLive.layer ++ mockRepo(Model(IdTwo))))
          assertRoute2 <- assertM(Put("/models", Model(IdTwo)) ~> api2.routes)(
            handled(
              status(equalTo(StatusCodes.OK)) ?? "Model(IdTwo)"
            )
          )
        } yield assertRoute1 && assertRoute2
      }
    )
}
