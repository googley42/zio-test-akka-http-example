package endpoint

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Route
import zio.{Runtime, ZIO}

abstract class ZioSupport(runtime: Runtime[AppEnv]) {

  implicit def zio2Marshaller[A](
    implicit m1: Marshaller[A, HttpResponse],
    m2: Marshaller[Throwable, HttpResponse]
  ): Marshaller[ZIO[Any, Throwable, A], HttpResponse] =
    Marshaller { implicit ec => a =>
      runtime.unsafeRun(a.fold(e => m2(e), a => m1(a)))
    }

  implicit def zioRouteMarshaller(zioRoute: ZIO[AppEnv, Throwable, Route]): Route = runtime.unsafeRun(zioRoute)
}
