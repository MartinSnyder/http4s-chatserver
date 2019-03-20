package com.martinsnyder.chatserver

sealed trait InputMessage
case class Chat(message: String) extends InputMessage
case object EnterDefaultRoom extends InputMessage
case class EnterRoom(room: String) extends InputMessage
case class InvalidInput(msg: String) extends InputMessage

object InputMessage {
  def parse(text: String): InputMessage =
    splitFirstTwoWords(text) match {
      case ("/room", "", "") => EnterDefaultRoom
      case ("/room", room, "") => EnterRoom(room)
      case ("/room", _, _) => InvalidInput("/room takes a single, optional argument")
      case _ => Chat(text)
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