package com.martinsnyder.chatserver

import java.io.File

import cats.effect.{ContextShift, Sync}
import io.circe.Json
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scala.concurrent.ExecutionContext.global

class HelloWorldRoutes[F[_]: Sync: ContextShift] extends Http4sDsl[F] {
  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case request @ GET -> Root  => StaticFile.fromFile(new File("static/index.html"), global, Some(request)).getOrElseF(NotFound())
      case request @ GET -> Root / "chat.js"  => StaticFile.fromFile(new File("static/chat.js"), global, Some(request)).getOrElseF(NotFound())

      case GET -> Root / "hello" / name =>
        Ok(Json.obj("message" -> Json.fromString(s"Hello, ${name}")))
    }
}
