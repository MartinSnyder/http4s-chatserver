# http4s-chatserver [![Build Status](https://travis-ci.org/MartinSnyder/http4s-chatserver.svg?branch=master)](https://travis-ci.org/MartinSnyder/http4s-chatserver)
A chat server implemented using [WebSockets][wikipedia] and [http4s][http4s]. Note that the version of this project mimics http4s itself, so the tags on the project indicate what versions this example has been built against. It was initially developed against 0.20.0-M6.

A working demo of this project is available here: [https://martinsnyder.net/projects/chat.html](https://martinsnyder.net/projects/chat.html).

This project is a practical example of:
* How to work with WebSockets in http4s
* Implementing application state using fs2 streams
* The publish/subscribe pattern

## Presentations

* [A Websocket-based Chat Server in http4s](https://nescala.io/talks.html#http4s-chat-server) ([video](https://www.youtube.com/watch?v=rB5RM-dc4Sg))
* [Live Coding a Chat Server with WebSockets and http4s](https://www.meetup.com/scala-phase/events/259959798/) ([video](https://www.youtube.com/watch?v=py_V_7gD5WU)) ([code](https://github.com/MartinSnyder/phase-http4s))

## Messages are processed as follows:
### In ChatRoutes (processInput)
1. Message received via WebSocket
2. Message text parsed into InputMessage
3. InputMessage routed to Queue
### In ChatServer (processingStream)
4. Queued InputMessages pumped through the current ChatState, producing a "next" state and some number of OutputMessage objects.
5. "Next" state is managed by a functional mutable reference (Ref)
6. OutputMessage objects are routed to a topic (publish/subscribe pattern)
### In ChatRoutes (toClient)
7. OutputMessage objects are received from the topic
8. User-specific filter is applied to the stream
9. Message text is routed to the WebSocket

## Notes
### Concurrency in fs2
This implementation relies heavily on the concurrency objects Ref, Queue and Topic. They are
used both to move messages between streams and also to control the flow of messages in general.

A more complicated implementation would queue on a per-room basis. That would require per-room
queues though, and would distract from what this example is trying to demonstrate.

### Limitations
The biggest weakness of what I've done here is that *all* traffic is routed through a single queue.
This neutralizes many of the benefits of http4s. Queueing is necessary in this case because we
need to sequence our messages in order to conduct the state transformations on ChatState. A more
complicated model would use multiple queues (and potentially multiple state objects), perhaps
along room boundaries.

### Functional enhancements
Functional improvements to this application can be implemented by modifying InputMessage,
OutputMessage and ChatState. These three classes are vanilla Scala and have no fs2 or http4s
dependencies. That means that 100% of the functionality can be trivially unit-tested
or evaluated in the REPL by a novice Scala programmer.

### Further reading
* [fs2 Documentation - Concurrency Primitives][fs2-concurrency]
* [http4s WebSocket Example][blaze-ws-example]
* [Ref objects in Cats Effect][cats-effect-ref]

[wikipedia]: https://en.wikipedia.org/wiki/WebSocket
[http4s]: https://http4s.org/
[fs2-concurrency]: https://fs2.io/concurrency-primitives.html
[blaze-ws-example]: https://github.com/http4s/http4s/blob/master/examples/blaze/src/main/scala/com/example/http4s/blaze/BlazeWebSocketExample.scala
[cats-effect-ref]: https://typelevel.org/cats-effect/concurrency/ref.html
