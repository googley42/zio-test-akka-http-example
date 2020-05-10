import zio.Has
import zio.logging.Logging

package object endpoint {
  type Repository = Has[Repository.Service]

  type AppEnv = Logging with Repository
}
