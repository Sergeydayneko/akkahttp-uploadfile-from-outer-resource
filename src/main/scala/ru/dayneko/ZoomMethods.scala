package ru.dayneko

import akka.http.scaladsl.model.StatusCodes.{OK, Unauthorized}
import akka.http.scaladsl.model.StatusCode
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by s.dayneko 31.07.2018
  */
trait Methods {
  var sessionId: String = _

  /**
    * Статус авторизации
    * @param rb response
    * @return
    */
  def getLogStatus(rb: String): StatusCode = if (rb.indexOf("</ok>") > 0) OK else Unauthorized

  /**
    * Извлекаем sessionId
    * @param s response
    * @return
    */
  def getSessionId(s: String): String = "sessionid=\"([\\w|\\d]+)\"".r.findFirstMatchIn(s).get.group(1)

  /**
    * Check token for protected resources
    * @param t is Coockie value from client
    * @return
    */
  def checkToken(t: String): Boolean = t.equals(sessionId)

  /**
    * Get couple id's
    * @param file
    * @return
    */
  def parseFile(file: Map[String, String]): String = file.get("file")
                                                        .map(_
                                                          .replaceAll("(?:couple_id=|\\n|\\r\\n)", " ")
                                                          .trim)
                                                        .getOrElse("")
                                                        .split("\\s+")
                                                        .mkString(",")

  /**
    * Parse token from second request
    * @param s
    * @return
    */
  def parseToken(s: String): String = "<reply>([\\w|\\d]+)</reply>".r.findFirstMatchIn(s).get.group(1)
}

trait Requests {
  case class NotFoundResponse(code: Int, `type`: String, message: String)
  case class UserPwd(login: String, password: String)
}

trait JsonProtocols extends DefaultJsonProtocol with Requests {
  implicit val idFormat: RootJsonFormat[UserPwd] = jsonFormat2(UserPwd.apply)
}
