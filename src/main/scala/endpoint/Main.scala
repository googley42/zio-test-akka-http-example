package endpoint

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import endpoint.model.Model
import zio._
import zio.console.Console

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
        refRecords <- Ref.make[Vector[Model]](Vector.empty)
        refFailOnIdList <- Ref.make[Vector[Int]](Vector.empty)
        api = new Api(Runtime.unsafeFromLayer(layer(refRecords, refFailOnIdList)))
        server <- ZIO.fromFuture(_ => Http().bindAndHandle(api.routes, "localhost", 8000))
      } yield server
    }
  } yield server

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (for {
      _ <- console.putStrLn("Starting Akka HTTP server endpoint...")
      exitCode <- httpServer.useForever.as(0)
    } yield exitCode).orDie

  private def layer(refDb: Ref[Vector[Model]], refFailOnIdList: Ref[Vector[Int]]) =
    Console.live >>> InMemoryRepository.inMemory(refDb, refFailOnIdList)

}
