package com.martinsnyder.chatserver

trait OutputMessage {
  def forUser(targetUser: String): Boolean
}

case class SendToUser(user: String, text: String) extends OutputMessage {
  def forUser(targetUser: String) = targetUser == user
}

case class SendToUsers(users: Set[String], text: String) extends OutputMessage {
  def forUser(targetUser: String) = users.contains(targetUser)
}
