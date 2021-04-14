# zio-test-akka-http-example

An example akka-http server written in ZIO that uses the excellent [`zio-test-akka-http`](https://github.com/senia-psm/zio-test-akka-http)
which provides integration of `akka-http-test` with `zio-test` via a simple DSL.

It provides some simple CRUD endpoints that use an in-memory DB, and also provides some examples of testing
using ZIO mocks.