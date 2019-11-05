package com.martinsnyder.chatserver

/*
 * Trait for any input operation that could come from the user
 */
sealed trait InputMessage {
  val user: String
}

case class Help(user: String)                       extends InputMessage
case class Chat(user: String, text: String)         extends InputMessage
case class EnterRoom(user: String, room: String)    extends InputMessage
case class ListRooms(user: String)                  extends InputMessage
case class ListMembers(user: String)                extends InputMessage
case class Disconnect(user: String)                 extends InputMessage
case class InvalidInput(user: String, text: String) extends InputMessage

object InputMessage {
  val DefaultRoomName = "default"
  val HelpText: String =
    """Commands:
      |  /help             - Show this text
      |  /room             - Change to default/entry room
      |  /room <room name> - Change to specified room
      |  /rooms            - List all rooms
      |  /members          - List members in current room
    """.stripMargin

  // Parses a string into a command
  def parse(user: String, text: String): InputMessage =
    splitFirstTwoWords(text) match {
      case ("/help", _, _)                    => Help(user)
      case ("/room", "", "")                  => EnterRoom(user, DefaultRoomName)
      case ("/room", room, "")                => EnterRoom(user, room.toLowerCase)
      case ("/room", _, _)                    => InvalidInput(user, "/room takes a single, optional argument")
      case ("/rooms", _, _)                   => ListRooms(user)
      case ("/members", _, _)                 => ListMembers(user)
      case (cmd, _, _) if cmd.startsWith("/") => InvalidInput(user, s"unknown command - $cmd")
      case _                                  => Chat(user, text)
    }

  private def splitFirstWord(text: String): (String, String) = {
    val trimmedText = text.trim
    val firstSpace  = trimmedText.indexOf(' ')
    if (firstSpace < 0)
      (trimmedText, "")
    else
      (trimmedText.substring(0, firstSpace), trimmedText.substring(firstSpace + 1).trim)
  }

  private def splitFirstTwoWords(text: String): (String, String, String) = {
    val (first, intermediate) = splitFirstWord(text)
    val (second, rest)        = splitFirstWord(intermediate)

    (first, second, rest)
  }
}
