package com.martinsnyder.chatserver

import java.io.File

import cats.effect.{ContextShift, Sync}
import fs2.Stream
import fs2.concurrent.{Queue, Topic}
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Text}

import scala.concurrent.ExecutionContext.global

/*
 * Processes single HTTP requests
 */
class HelloWorldRoutes[F[_]: Sync: ContextShift](queue: Queue[F, InputMessage], topic: Topic[F, OutputMessage]) extends Http4sDsl[F] {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      // Static resources
      case request @ GET -> Root  => StaticFile.fromFile(new File("static/index.html"), global, Some(request)).getOrElseF(NotFound())
      case request @ GET -> Root / "chat.js"  => StaticFile.fromFile(new File("static/chat.js"), global, Some(request)).getOrElseF(NotFound())

      case GET -> Root / "ws" / userName =>
        // Routes messages from our "topic" to a WebSocket
        val toClient: Stream[F, WebSocketFrame.Text] =
          topic
            .subscribe(1000)
            .filter(_.forUser(userName))
            .map(msg => Text(msg.toString))

        // Pipe that takes client messages from a WebSocket and routes them to our Queue
        def processInput(wsfStream: Stream[F, WebSocketFrame]): Stream[F, Unit] = {
          // Stream of initialization events for a user
          val entryStream: Stream[F, InputMessage] = Stream.emits(Seq(EnterRoom(userName, InputMessage.DefaultRoomName)))

          // Stream that transforms between raw text from the client and parsed InputMessage objects
          val parsedWebSocketInput: Stream[F, InputMessage] =
            wsfStream
              .map({
                case Text(text, _) => InputMessage.parse(userName, text)
                case Close(_) => Disconnect(userName)

                // Intentionally let other message types throw a MatchError
                // these types are not expected to ever happen
              })

          (entryStream ++ parsedWebSocketInput).through(queue.enqueue)
        }

        // Build the WebSocket handler
        WebSocketBuilder[F].build(toClient, processInput)
    }
}
