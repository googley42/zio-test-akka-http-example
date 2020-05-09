package endpoint

import zio.test.mock.Mockable

object Foo {}

@Mockable[Repository.Service]
object RepositoryMock
