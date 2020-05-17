package endpoint

import akka.http.scaladsl.model.StatusCodes
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import endpoint.model.{Id, Model}
import zio.clock.Clock
import zio.console.Console
import zio.logging.Logging
import zio.test.Assertion._
import zio.test._
import zio.test.akkahttp.DefaultAkkaRunnableSpec
import zio.{Ref, Runtime, ZIO, ZLayer}
import zio.test.environment._

object ApiSpec2 extends DefaultAkkaRunnableSpec {
  import FailFastCirceSupport._
  import io.circe.generic.auto._

  private val IdOne = Id("1")

  override def spec =
    suite("ApiSpec")(
      testM("get route returns entity and logs with correlation-id") {
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
          startsWithString(
            "1970-01-01T00:00Z INFO  [correlation-id = 6c7dcaa9-e383-4993-be20-b8dd1949e19f] getting record Id(1)"
          )
        )
      }
    )

  private def apiLayer(refMap: Ref[Map[Id, Model]]) =
    for {
      clock <- ZIO.environment[Clock]
      clockLayer = ZLayer.succeed[Clock.Service](clock.get)
      console <- ZIO.environment[Console]
      consoleLayer = ZLayer.succeed[Console.Service](console.get)
      logLayer = consoleLayer ++ clockLayer >>> LoggingLive.testLayer
      apiLayer = logLayer ++ (logLayer >>> InMemoryRepository.inMemory(refMap))
    } yield apiLayer

}

class NoddyApi(r: Runtime[Logging with Repository])
