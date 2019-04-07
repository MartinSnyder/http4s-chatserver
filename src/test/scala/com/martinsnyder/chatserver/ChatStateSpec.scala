package com.martinsnyder.chatserver

import org.specs2.matcher.MatchResult

class ChatStateSpec extends org.specs2.mutable.Specification {
  "ChatState" >> {
    "initializes empty" >> {
      noUsers && noRooms
    }
    "welcomes new users (only)" >> {
      welcomeNewUser && noWelcomeOldUser
    }
  }

  private[this] def noUsers(): MatchResult[Map[_,_]] =
    ChatState().userRooms must beEqualTo(Map.empty)

  private[this] def noRooms(): MatchResult[Map[_,_]] =
    ChatState().roomMembers must beEqualTo(Map.empty)

  private[this] def welcomeNewUser(): MatchResult[OutputMessage] =
    ChatState().process(EnterRoom("testUser", "room"))._2.head must beEqualTo(WelcomeUser("testUser"))

  private[this] def noWelcomeOldUser(): MatchResult[Seq[OutputMessage]] = {
    val initState = ChatState().process(EnterRoom("testUser", "room"))._1
    initState.process(EnterRoom("testUser", "room"))._2 must beEqualTo(Seq())
  }
}
