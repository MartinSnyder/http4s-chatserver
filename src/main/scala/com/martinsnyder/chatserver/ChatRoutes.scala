package com.martinsnyder.chatserver

import java.io.File
import java.util.concurrent.Executors

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, ContextShift, Sync}
import fs2.concurrent.{Queue, Topic}
import fs2.{Pipe, Stream}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Text}
import org.http4s.{HttpRoutes, MediaType, StaticFile}

/*
 * Processes single HTTP requests
 */
class ChatRoutes[F[_]: Sync: ContextShift](
    chatState: Ref[F, ChatState],
    queue: Queue[F, InputMessage],
    topic: Topic[F, OutputMessage]
) extends Http4sDsl[F] {

  private val blocker = {
    val numBlockingThreadsForFilesystem = 4
    val blockingPool                    = Executors.newFixedThreadPool(numBlockingThreadsForFilesystem)
    Blocker.liftExecutorService(blockingPool)
  }

  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      // Static resources
      case request @ GET -> Root =>
        StaticFile
          .fromFile(new File("static/index.html"), blocker, Some(request))
          .getOrElseF(NotFound())

      case request @ GET -> Root / "chat.js" =>
        StaticFile
          .fromFile(new File("static/chat.js"), blocker, Some(request))
          .getOrElseF(NotFound())

      // Read the current state and format some stats in HTML
      case GET -> Root / "metrics" =>
        val outputStream: Stream[F, String] = Stream
          .eval(chatState.get)
          .map(state => s"""
               |<html>
                 |<title>Chat Server State</title>
                 |<body>
                   |<div>Users: ${state.userRooms.keys.size}</div>
                   |<div>Rooms: ${state.roomMembers.keys.size}</div>
                 |</body>
               |</html>
              """.stripMargin)

        Ok(outputStream, `Content-Type`(MediaType.text.html))

      // Bind a WebSocket connection for a user
      case GET -> Root / "ws" / userName =>
        // Routes messages from our "topic" to a WebSocket
        val toClient: Stream[F, WebSocketFrame.Text] =
          topic
            .subscribe(1000)
            .filter(_.forUser(userName))
            .map(msg => Text(msg.toString))

        // Function that converts a stream of one type to another. Effectively an external "map" function
        def processInput(wsfStream: Stream[F, WebSocketFrame]): Stream[F, Unit] = {
          // Stream of initialization events for a user
          val entryStream: Stream[F, InputMessage] = Stream.emits(Seq(EnterRoom(userName, InputMessage.DefaultRoomName)))

          // Stream that transforms between raw text from the client and parsed InputMessage objects
          val parsedWebSocketInput: Stream[F, InputMessage] =
            wsfStream
              .collect {
                case Text(text, _) => InputMessage.parse(userName, text)

                // Convert the terminal WebSocket event to a User disconnect message
                case Close(_) => Disconnect(userName)
              }

          // Create a stream that has all of the user input sandwiched between the entry and disconnect messages
          (entryStream ++ parsedWebSocketInput).through(queue.enqueue)
        }

        // WebSocketBuilder needs a "pipe" which is a type alias for a stream transformation function like processInput above
        // This variable is not necessary to compile, but is included to clarify the exact type of Pipe.
        val inputPipe: Pipe[F, WebSocketFrame, Unit] = processInput

        // Build the WebSocket handler
        WebSocketBuilder[F].build(toClient, inputPipe)
    }
}
