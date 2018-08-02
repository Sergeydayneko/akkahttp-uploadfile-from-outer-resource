package ru.dayneko

import com.softwaremill.session.{SessionSerializer, SingleValueSessionSerializer}
import scala.util.Try

/**
  * Created by s.dayneko 01.08.2018
  */
case class SessionToken(sessionId: String)

object SessionToken {
  implicit def serializer: SessionSerializer[SessionToken, String] =
    new SingleValueSessionSerializer(_.sessionId, (token: String) => Try { SessionToken(token) })
}
