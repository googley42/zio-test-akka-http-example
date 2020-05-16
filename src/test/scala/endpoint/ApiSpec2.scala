package endpoint

import endpoint.model.{Id, Model}
import zio.logging.{Logger, Logging}
import zio.test._
import zio.test.akkahttp.DefaultAkkaRunnableSpec
import zio.test.akkahttp.RouteTestEnvironment.TestEnvironment
import zio.{Has, Ref, Runtime, ZEnv, ZIO, ZLayer}
import zio.logging.Logging

object ApiSpec2 extends DefaultAkkaRunnableSpec {

  private val IdOne = Id("1")
  private val IdTwo = Id("2")

  import zio.test.environment._
  val loggingLayer: ZLayer[zio.ZEnv, Nothing, Logging] = ((Live.default >>> TestConsole.debug) ++ (Live.default >>> TestClock.default)) >>> LoggingLive.testLayer
  val yyy: ZLayer[Any, Nothing, Logging] = ZEnv.live >>> loggingLayer

  private def layer(refMap: Ref[Map[Id, Model]]): ZLayer[Any, Nothing, Logging with Repository] =
    yyy ++ (yyy >>> InMemoryRepository.inMemory(refMap))

  private def layer2(
    refMap: Ref[Map[Id, Model]],
    logging: ZLayer[Any, Nothing, Logging]
  ): ZLayer[Any, Nothing, Logging with Repository] =
    logging ++ (logging >>> InMemoryRepository.inMemory(refMap))

  val x: ZLayer[Logging, Nothing, Logging] = ZLayer.requires[Logging]
  val y: ZLayer[Logging, Nothing, Logging] = ZLayer.identity[Logging]
  val z: ZLayer[Logging, Nothing, Logging] = ZLayer.service[Logger[String]]

  override def spec =
    suite("ApiSpec")(
      testM("") {
        for {
          _ <- Logging.info("XXX")
          log <- ZIO.environment[Logging]
          logLayer = ZLayer.succeed[Logger[String]](log.get)
          vector <- TestConsole.output
          r = new NoddyApi(Runtime.unsafeFromLayer(layer2(null, logLayer)))
          _ <- ZIO.effect(println(s">${vector.head}<"))
        } yield assertCompletes
      }.provideCustomLayer(LoggingLive.testLayer)
    )
}

class NoddyApi(r: Runtime[Logging with Repository])
