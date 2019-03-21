package com.martinsnyder.chatserver

trait OutputMessage {
  def forUser(targetUser: String): Boolean
  def toString: String
}

case class SendToUser(user: String, text: String) extends OutputMessage {
  override def forUser(targetUser: String) = targetUser == user
  override def toString = text
}

case class SendToUsers(users: Set[String], text: String) extends OutputMessage {
  override def forUser(targetUser: String) = users.contains(targetUser)
  override def toString = text
}
