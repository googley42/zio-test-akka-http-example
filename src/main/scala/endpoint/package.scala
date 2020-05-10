import zio.Has
import zio.logging.Logging

package object endpoint {
//  type Config = Has[Config.Service]
//  type EndpointClient = Has[EndpointClient.Service]
  type Repository = Has[Repository.Service]
//  type Repository2 = Has[Repository2.Service]

  type AppEnv = Repository
}
