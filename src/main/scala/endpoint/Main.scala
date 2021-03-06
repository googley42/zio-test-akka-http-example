package endpoint

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import endpoint.model.{Id, Model}
import zio._
import zio.console.Console
import zio.logging.LogAnnotation
import zio.logging.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object Main extends App {

  private val managedActorSystem = ZManaged.makeEffect(ActorSystem("EndpointApp"))(_.terminate())

  private val httpServer: ZManaged[Any, Throwable, Http.ServerBinding] = for {
    actorSystem <- managedActorSystem
    server <- ZManaged.fromEffect {
      implicit val as: ActorSystem = actorSystem
      implicit val mat: ActorMaterializer = ActorMaterializer()
      implicit val ec: ExecutionContext = platform.executor.asEC
      for {
        refMap <- Ref.make[Map[Id, Model]](Map.empty)
        api = new Api(Runtime.unsafeFromLayer(AppLogging.layer ++ layer(refMap)))
        server <- ZIO.fromFuture(_ => Http().bindAndHandle(api.routes, "localhost", 8000))
      } yield server
    }
  } yield server

  private def layer(refMap: Ref[Map[Id, Model]]) =
    (AppLogging.layer ++ Console.live) >>> InMemoryRepository.inMemory(refMap)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for {
      _ <- console.putStrLn("Starting Akka HTTP server endpoint...")
      _ <- httpServer.useForever
    } yield ()).orDie.exitCode
}
