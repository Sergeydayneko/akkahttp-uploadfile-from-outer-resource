package ru.dayneko

import akka.stream.ActorMaterializer
import org.slf4j.{Logger, LoggerFactory}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}
import akka.http.scaladsl.server._
import StatusCodes._
import akka.http.scaladsl.model.headers.{Cookie, RawHeader}
import akka.http.scaladsl.server.Directives._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

/**
  * Created by s.dayneko 01.08.2018
  */
object Main extends App with JsonProtocols with Methods {
  val log: Logger                                 = LoggerFactory.getLogger(this.getClass)
  val sender: HttpRequest => Future[HttpResponse] = createRequestSender()

  /**
    * Actor system initialize
    */
  implicit val system: ActorSystem                        = ActorSystem("login-system")
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  /**
    * Session object initialize
    */
  val sessionConfig: SessionConfig                                           = SessionConfig.default("0LrQsNC60L7QuS3QvdC40LHRg9C00Ywt0YLQsNC8LdGB0LXQutGA0LXRgi3QutCw0LrQvtC5LdC90LjQsdGD0LTRjC3RgtCw0Lwt0YHQtdC60YDQtdGCLdC60LDQutC+0Lkt0L3QuNCx0YPQtNGMLdGC0LDQvC3RgdC10LrRgNC10YIt0LrQsNC60L7QuS3QvdC40LHRg9C00Ywt0YLQsNC8LdGB0LXQutGA0LXRgg==")
  implicit val sessionManager: SessionManager[SessionToken]                   = new SessionManager[SessionToken](sessionConfig)
  implicit val refreshTokenStorage: InMemoryRefreshTokenStorage[SessionToken] = new InMemoryRefreshTokenStorage[SessionToken] {
    override def log(msg: String): Unit = println(s"""$msg for current storage session token""")
  }
  val myRequiredSession                 = requiredSession(refreshable, usingCookies)
  val myInvalidateSession               = invalidateSession(refreshable, usingCookies)
  def mySetSession(value: SessionToken): Directive0 = setSession(refreshable, usingCookies, value)

  implicit def rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handleAll[MethodRejection] { methodRejections =>
      val supportedMethods = methodRejections.map(_.supported.name)
      complete((MethodNotAllowed, s"Метод не поддерживается! Поддерживаемые запросы по данному адресу: ${supportedMethods.mkString(" или ")}"))
    }
      .handleNotFound {
        val notFoundResponse: String = NotFoundResponse(NotFound.intValue, "NotFound", "Запрашиваемая страница не найдена.").message
        complete(HttpResponse(NotFound, entity = HttpEntity(ContentTypes.`application/json`, notFoundResponse)))
      }
      .result()

  /**
    * Routes for auth and file upload
    */
  val route =
      path("login") {
        get {
          getFromResource("frontend/login.html")
        } ~
        post {
          entity(as[UserPwd]) { body =>
            val result = sender(HttpRequest(uri = Uri(s"http://127.0.0.1:8080/calls/loginserver" +
              s"loginname=${body.login}" +
              s"&password=${body.password}")))
              .flatMap(s => Unmarshal(s.entity).to[String])
              .map(r => {(getLogStatus(r), getSessionId(r))
              })

            onComplete(result) {
              case Success(s: (StatusCode, String))  =>
                log.info("Successfullu passed data to server with login = {}", body.login)
                mySetSession(SessionToken(s._2)) {{ ctx => ctx.complete(s._1) }}
              case Failure(ex) =>
                log.error("Error occurred while logging in", ex)
                complete(InternalServerError)
            }
          }
        }
      } ~
        path("main") {
          get {
            myRequiredSession { session =>
              log.info("successfully logged in main page")
              getFromResource("frontend/upload.html")
            }
          }
        } ~
          path("fileUpload") {
            post {
              myRequiredSession { session =>
                formFieldMap { fields =>
                  val idCalls: String = parseFile(fields)
                  val cookieHeader: Cookie = akka.http.scaladsl.model.headers.Cookie("JSESSIONID", session.sessionId)

                  val res: Future[HttpResponse] = sender(HttpRequest(uri = Uri(s"http://127.0.0.1:8080/calls/uploadtoken;jsessionId=$sessionId?id_call[]=$idCalls&type=1&action=download"), headers = List(cookieHeader)))
                    .flatMap(s => Unmarshal(s.entity).to[String])
                    .map(s => parseToken(s))
                    .map((session.sessionId, _))
                    .flatMap({ case (s, t) =>
                      val cookieHeader = akka.http.scaladsl.model.headers.Cookie("JSESSIONID", s)
                      sender(HttpRequest(uri = Uri(s"http://127.0.0.1:8080/calls/sendcallfile.mp3;jsessionId=$s?token=$t&id_call[]=$idCalls&type=1&action=download"), headers = List(cookieHeader)))
                    })

                  onComplete(res) {
                    case Success(r: HttpResponse) =>
                      complete{
                        val contentType: ContentType = ContentTypes.`application/octet-stream`
                        val dispositionHeader = RawHeader("Content-Disposition", "attachment; filename=uploadedFile.zip")
                        HttpResponse(OK, entity = HttpEntity(contentType, r.entity.dataBytes)).withHeaders(dispositionHeader)
                      }
                  }
                }
              }
            }
          }


  /**
    * redirect from empty url, logout, css and js dependencies
    */
  val additionalRoutes: Route =
    path(Remaining) { resource =>
      getFromResource(resource)
    } ~
    path("logout") {
      post {
        myRequiredSession { session =>
          myInvalidateSession { ctx =>
            ctx.complete("ok")
          }
        }
      }
    } ~
    path("") {
      redirect("/login", Found)
    }

  val bindingFuture = Http().bindAndHandle(route ~ additionalRoutes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

  /**
    * Создание объекта для Http запросов
    * private
    * @return
    */
  private def createRequestSender(): HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
}
