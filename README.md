# http4s-chatserver
Example chat server implemented using WebSockets on http4s 0.20

This project is intended to be a practical example of how to work with WebSockets in http4s.
Specifically, it implements a stateful server that all WebSocket clients interact with.

## Messages are processed as follows:
### In ChatRoutes (processInput)
1. Message received via WebSocket
2. Message text parsed into InputMessage
3. InputMessage routed to Queue
### In ChatServer (processingStream)
4. Queued InputMessages pumped through the current ChatState, producing a "next" state and some number of OutputMessage objects.
5. "Next" state is preserved for the next iteration
6. OutputMessage objects are routed to a topic (publish/subscribe pattern)
### In ChatRoutes (toClient)
7. OutputMessage objects are received from the topic
8. User-specific filter is applied to the stream
9. Message text is routed to the WebSocket

## Notes
### Concurrency in fs2
This implementation relies heavily on the concurrency objects Queue and Topic. The biggest
weakness of what I've done here is that *all* traffic is routed through a single queue. This
neutralizes many of the benefits of http4s. Queueing is necessary in this case because we
need to sequence our messages in order to conduct the state transformations on ChatState.

A more complicated implementation would queue on a per-room basis. That would require per-room
queues though, and would distract from what this example is trying to demonstrate.
### Functional enhancements
Functional enhancements to this chat server can be implemented by modifying InputMessage,
OutputMessage and ChatState. These three classes are vanilla Scala and have no fs2 or http4s
dependencies. Of note, that means that 100% of the functionality can be trivially unit-tested
or evaluated in the REPL by a novice Scala programmer.