package endpoint

import endpoint.model.Id
import zio.logging.Logging
import zio.test._
import zio.test.akkahttp.DefaultAkkaRunnableSpec
import zio.test.akkahttp.RouteTestEnvironment.TestEnvironment
import zio.{Runtime, ZEnv, ZIO, ZLayer}

object ApiSpec2 extends DefaultAkkaRunnableSpec {

  private val IdOne = Id("1")
  private val IdTwo = Id("2")

  import zio.test.environment._
  val loggingLayer: ZLayer[zio.ZEnv, Nothing, Logging] = ((Live.default >>> TestConsole.debug) ++ (Live.default >>> TestClock.default)) >>> LoggingLive.testLayer
  val yyy: ZLayer[Any, Nothing, Logging] = ZEnv.live >>> loggingLayer

  override def spec =
    suite("ApiSpec")(
      testM("") {
        for {
          _ <- Logging.info("XXX")
          vector <- TestConsole.output
          r = new NoddyApi(Runtime.unsafeFromLayer(yyy))
          _ <- ZIO.effect(println(s">${vector.head}<"))
        } yield assertCompletes
      }.provideCustomLayer(LoggingLive.testLayer)
    )
}

class NoddyApi(r: Runtime[Logging])
class NoddyApi2(r: Runtime[TestEnvironment])
