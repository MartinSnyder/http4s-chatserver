package com.martinsnyder.chatserver

case class ChatState(userRooms: Map[String, String], roomMembers: Map[String, Set[String]]) {
  def process(user: String, msg: InputMessage): (ChatState, Seq[OutputMessage]) = msg match {
    case Chat(text) => userRooms.get(user) match {
      case Some(room) =>
        (this, sendToRoom(room, s"$user: $text"))

      case None =>
        (this, Seq(SendToUser(user, "You are not currently in a room")))
    }

    case EnterRoom(toRoom) =>
      val nextState = removeFromCurrentRoom(user).addToRoom(user, toRoom)
      val leaveMessage = userRooms
        .get(user)
        .toSeq
        .flatMap(fromRoom => sendToRoom(fromRoom, s"$user has left $fromRoom"))
      val enterMessage = sendToRoom(toRoom, s"$user has joined $toRoom")

      (nextState, leaveMessage ++ enterMessage)

    case InvalidInput(text) =>
      (this, Seq(SendToUser(user, s"Invalid input: $text")))
  }

  private def sendToRoom(room: String, text: String): Seq[OutputMessage] = {
    userRooms
      .get(room)
      .flatMap(roomMembers.get)
      .map(SendToUsers(_, text))
      .toSeq
  }

  private def removeFromCurrentRoom(user: String): ChatState = userRooms.get(user) match {
    case Some(room) =>
      val nextMembers = roomMembers.getOrElse(room, Set()) - user

      ChatState(userRooms - user, roomMembers + (room -> nextMembers))
    case None =>
      this
  }

  private def addToRoom(user: String, room: String): ChatState = {
    val nextMembers = roomMembers.getOrElse(room, Set()) + user

    ChatState(userRooms + (user -> room), roomMembers + (room -> nextMembers))
  }
}
