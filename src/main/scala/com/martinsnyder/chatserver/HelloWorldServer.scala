package com.martinsnyder.chatserver

import cats.effect._
import cats.implicits._
import fs2.concurrent.Topic
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

object HelloWorldServer extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    for (
      topic <- Topic[IO, String]("initialized");
      exitCode <- ServerStream.stream[IO](topic).compile.drain.as(ExitCode.Success)
    ) yield exitCode
}

object ServerStream {
  def helloWorldRoutes[F[_]: Effect: ContextShift](topic: Topic[F, String]): HttpRoutes[F] =
    new HelloWorldRoutes[F](topic).routes

  def stream[F[_]: ConcurrentEffect: Timer: ContextShift](topic: Topic[F, String]): fs2.Stream[F, ExitCode]=
    BlazeServerBuilder[F]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(Router(
        "/" -> helloWorldRoutes(topic)
      ).orNotFound)
      .serve
}
