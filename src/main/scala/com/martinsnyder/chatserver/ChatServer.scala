package com.martinsnyder.chatserver

import scala.util.Try
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2.Stream
import fs2.concurrent.{Queue, Topic}
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

/*
 * Top-level executor for our application
 */
object HelloWorldServer extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    // Read a potential tcp port from the command line
    val httpPort = args
      .headOption
      .orElse(sys.env.get("PORT"))
      .flatMap(s => Try(s.toInt).toOption)
      .getOrElse(8080)

    for (
      // Synchronization objects must be created at the top-level so they can be shared across the application
      ref <- Ref.of[IO, ChatState](ChatState());
      queue <- Queue.unbounded[IO, InputMessage];
      topic <- Topic[IO, OutputMessage](SendToUsers(Set.empty, ""));
      exitCode <- {
        import scala.concurrent.duration._

        // Stream for HTTP requests
        val httpStream = ServerStream.stream[IO](httpPort, queue, topic)

        // Stream to keep alive idle WebSockets
        val keepAlive = Stream.awakeEvery[IO](30.seconds).map(_ => KeepAlive).through(topic.publish)

        // Stream to process items from the queue and publish the results to the topic
        // 1. Dequeue
        // 2. apply message to state reference
        // 3. Convert resulting output messages to a stream
        // 4. Publish output messages to the publish/subscribe topic
        val processingStream =
          queue
            .dequeue
            .flatMap(msg => Stream.eval(ref.modify(_.process(msg))))
            .flatMap(Stream.emits)
            .through(topic.publish)

        // fs2 Streams must be "pulled" to process messages. Drain will perpetually pull our top-level streams
        Stream(httpStream, keepAlive, processingStream)
          .parJoinUnbounded
          .compile
          .drain
          .as(ExitCode.Success)
      }

    ) yield exitCode
  }
}

object ServerStream {
  def helloWorldRoutes[F[_]: Effect: ContextShift](queue: Queue[F, InputMessage], topic: Topic[F, OutputMessage]): HttpRoutes[F] =
    new ChatRoutes[F](queue, topic).routes

  // Builds a stream for HTTP events processed by our router
  def stream[F[_]: ConcurrentEffect: Timer: ContextShift](port: Int, queue: Queue[F, InputMessage], topic: Topic[F, OutputMessage]): fs2.Stream[F, ExitCode]=
    BlazeServerBuilder[F]
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(Router(
        "/" -> helloWorldRoutes(queue, topic)
      ).orNotFound)
      .serve
}
