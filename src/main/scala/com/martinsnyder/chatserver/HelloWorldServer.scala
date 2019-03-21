package com.martinsnyder.chatserver

import cats.effect._
import cats.implicits._
import fs2.Stream
import fs2.concurrent.{Queue, Topic}
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

object HelloWorldServer extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    for (
      queue <- Queue.unbounded[IO, InputMessage];
      topic <- Topic[IO, OutputMessage](SendToUsers(Set.empty, ""));
      exitCode <- {
        val httpStream = ServerStream.stream[IO](queue, topic)
        val processingStream =
          queue
            .dequeue
            .mapAccumulate(ChatState(Map.empty, Map.empty))((prevState, inputMsg) => prevState.process(inputMsg))
            .map(_._2)
            .flatMap(Stream.emits)
            .through(topic.publish)

        httpStream.concurrently(processingStream).compile.drain.as(ExitCode.Success)
      }

    ) yield exitCode
}

object ServerStream {
  def helloWorldRoutes[F[_]: Effect: ContextShift](queue: Queue[F, InputMessage], topic: Topic[F, OutputMessage]): HttpRoutes[F] =
    new HelloWorldRoutes[F](queue, topic).routes

  def stream[F[_]: ConcurrentEffect: Timer: ContextShift](queue: Queue[F, InputMessage], topic: Topic[F, OutputMessage]): fs2.Stream[F, ExitCode]=
    BlazeServerBuilder[F]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(Router(
        "/" -> helloWorldRoutes(queue, topic)
      ).orNotFound)
      .serve
}
