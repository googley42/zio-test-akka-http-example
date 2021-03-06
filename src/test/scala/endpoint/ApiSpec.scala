package endpoint

import akka.http.scaladsl.model.StatusCodes
import endpoint.model.{Id, Model}
import zio.clock.Clock
import zio.console.Console
import zio.logging.Logging
import zio.test.Assertion._
import zio.test._
import zio.test.akkahttp.DefaultAkkaRunnableSpec
import zio.test.environment._
import zio.test.mock.Expectation._
import zio.{Ref, Runtime, ULayer, ZIO, ZLayer}

object ApiSpec extends DefaultAkkaRunnableSpec {
  import de.heikoseeberger.akkahttpziojson.ZioJsonSupport._

  private val IdOne = Id("1")
  private val IdTwo = Id("2")

  private val logLayer: ZIO[Console with Clock, Nothing, ZLayer[Any, Nothing, Logging]] = for {
    clock <- ZIO.environment[Clock]
    clockLayer = ZLayer.succeed[Clock.Service](clock.get)
    console <- ZIO.environment[Console]
    consoleLayer = ZLayer.succeed[Console.Service](console.get)
    logLayer = consoleLayer ++ clockLayer >>> AppLogging.testLayer
  } yield logLayer

  private def apiLayer(refMap: Ref[Map[Id, Model]]) =
    for {
      logLayer <- logLayer
      apiLayer = logLayer ++ (logLayer >>> InMemoryRepository.inMemory(refMap))
    } yield apiLayer

  override def spec =
    suite("ApiSpec")(
      testM("get all should return all records using assertM") {

        val result = for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          apiLayer <- apiLayer(refMap)
          api = new Api(Runtime.unsafeFromLayer(apiLayer))
          result <- Get("/models") ~> api.routes
        } yield result

        assertM(result)(
          handled(
            status(equalTo(StatusCodes.OK))
              && entityAs[Seq[Model]](isRight(equalTo(Seq(Model(IdOne)))))
          )
        )
      },
      testM("get should return OK and Model with IdOne and log with correlation id") {

        for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          apiLayer <- apiLayer(refMap)
          api = new Api(Runtime.unsafeFromLayer(apiLayer))
          assertRoute <- assertM(Get("/models/1") ~> api.routes)(
            handled(
              status(equalTo(StatusCodes.OK))
                && entityAs[Model](isRight(equalTo(Model(IdOne))))
            )
          )
          vector <- TestConsole.output
        } yield assertRoute && assert(vector.head)(
          startsWithString("1970-01-01 00:00:00.000Z info my-component getting record Id(1)")
        )
      },
      testM("delete should delete Model from repository") {
        for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          apiLayer <- apiLayer(refMap)
          api = new Api(Runtime.unsafeFromLayer(apiLayer))
          assertDeleteRoute <- assertM(Delete("/models/1") ~> api.routes)(
            handled(
              status(equalTo(StatusCodes.OK))
            )
          )
          assertModel <- assertM(refMap.get)(equalTo(Map.empty[Id, Model]))
        } yield assertDeleteRoute && assertModel
      },
      testM("get should return OK using assert") {
        for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty + (IdOne -> Model(IdOne)))
          apiLayer <- apiLayer(refMap)
          api = new Api(Runtime.unsafeFromLayer(apiLayer))
          assertGetRoute <- assertM(Get("/models/1") ~> api.routes)(
            handled(
              status(equalTo(StatusCodes.OK))
            )
          )
        } yield assertGetRoute
      },
      testM("put using in memory repo") {
        for {
          refMap <- Ref.make[Map[Id, Model]](Map.empty)
          apiLayer <- apiLayer(refMap)
          api = new Api(Runtime.unsafeFromLayer(apiLayer))
          assertPutRoute <- assertM(Put("/models", Model(IdOne)) ~> api.routes)(
            handled(
              status(equalTo(StatusCodes.OK))
            )
          )
        } yield assertPutRoute
      },
      testM("put using mocked repo") {
        val mockRepo: ULayer[Repository] = MockRepository
          .Get(equalTo(IdOne), value(Some(Model(IdOne)))) andThen MockRepository.Put(equalTo(Model(IdOne)), unit)

        for {
          log <- logLayer
          api = new Api(Runtime.unsafeFromLayer(log ++ mockRepo))
          assertPutRoute <- assertM(Put("/models", Model(IdOne)) ~> api.routes)(
            handled(
              status(equalTo(StatusCodes.OK))
            )
          )
        } yield assertPutRoute
      },
      testM("put using mocked repo") {

        def getAnyModel(model: Model) = MockRepository.Get(anything, value(Some(model)))
        val putAnyModel = MockRepository.Put(anything, unit)
        def mockRepo(model: Model): ULayer[Repository] =
          getAnyModel(model) andThen putAnyModel

        for {
          _ <- ZIO.unit
          log <- logLayer
          api1 = new Api(Runtime.unsafeFromLayer(log ++ mockRepo(Model(IdOne))))
          assertPutRoute1 <- assertM(Put("/models", Model(IdOne)) ~> api1.routes)(
            handled(
              status(equalTo(StatusCodes.OK)) ?? "Model(IdOne)"
            )
          )
          api2 = new Api(Runtime.unsafeFromLayer(log ++ mockRepo(Model(IdTwo))))
          assertPutRoute2 <- assertM(Put("/models", Model(IdTwo)) ~> api2.routes)(
            handled(
              status(equalTo(StatusCodes.OK)) ?? "Model(IdTwo)"
            )
          )
        } yield assertPutRoute1 && assertPutRoute2
      }
    )
}
