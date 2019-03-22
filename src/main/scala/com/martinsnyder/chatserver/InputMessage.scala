package com.martinsnyder.chatserver

sealed trait InputMessage {
  val user: String
}
case class Chat(user: String, text: String) extends InputMessage
case class EnterRoom(user: String, room: String) extends InputMessage
case class Disconnect(user: String) extends InputMessage
case class InvalidInput(user: String, text: String) extends InputMessage

object InputMessage {
  val DefaultRoomName = "default"

  def parse(user: String, text: String): InputMessage =
    splitFirstTwoWords(text) match {
      case ("/room", "", "") => EnterRoom(user, DefaultRoomName)
      case ("/room", room, "") => EnterRoom(user, room.toLowerCase)
      case ("/room", _, _) => InvalidInput(user, "/room takes a single, optional argument")
      case _ => Chat(user, text)
    }

  private def splitFirstWord(text: String): (String, String) = {
    val trimmedText = text.trim
    val firstSpace = trimmedText.indexOf(' ')
    if (firstSpace < 0)
      (trimmedText, "")
    else
      (trimmedText.substring(0, firstSpace), trimmedText.substring(firstSpace + 1).trim)
  }

  private def splitFirstTwoWords(text: String): (String, String, String) = {
    val (first, intermediate) = splitFirstWord(text)
    val (second, rest) = splitFirstWord(intermediate)

    (first, second, rest)
  }
}