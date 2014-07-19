package spray
package routing

import org.specs2.mutable.{
  Specification,
  After
}
import org.specs2.specification.Scope

import testkit.Specs2RouteTest

import directives.SessionDirectives

import session.SessionManager

import http._
import HttpHeaders._
import StatusCodes._

import scala.concurrent.duration.{
  Duration,
  SECONDS
}
import scala.concurrent.Future

import akka.util.Timeout
import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory

abstract class SessionSpec extends Specification with Specs2RouteTest {
  self =>

  implicit val timeout = new Timeout(Duration(20, SECONDS))

  def manager: SessionManager[Int]

  trait SessionApp extends HttpService with SessionDirectives[Int] with Scope with After {

    lazy val actorRefFactory = ActorSystem()

    implicit val ec = system.dispatcher

    val invalidSessionHandler = RejectionHandler {
      case InvalidSessionRejection(id) :: _ =>
        complete(Unauthorized, s"Unknown session $id")
    }

    // create a new manager for each scope
    val manager = self.manager

    def after = actorRefFactory.shutdown()

    val sessionRoute =
      handleRejections(invalidSessionHandler) {
          cookieSession { (id, map) =>
            setCookieSession(id) {
              get {
                val result = map.getOrElse("value", 0)
                updateSession(id, map.updated("value", result + 1)) {
                    complete(result.toString)
                  }
                } ~
                delete {
                  invalidateSession(id) {
                    complete("ok")
                  }
                }
            }
          }
      }

  }

  "Session state" should {

    "be created when no cookie is sent" in new SessionApp {
      Get("new") ~> sessionRoute ~> check {
        responseAs[String] === "0"
      }
    }

    "be kept between two request with same session id" in new SessionApp {
      Get("first") ~> sessionRoute ~> check {
        responseAs[String] === "0"
        val cookieOpt = header[`Set-Cookie`]
        cookieOpt should beSome
        val cookie = cookieOpt.get.cookie
        Get("second") ~> addHeader(Cookie(cookie)) ~> sessionRoute ~> check {
          responseAs[String] === "1"
          val cookieOpt = header[`Set-Cookie`]
          cookieOpt should beSome
          val cookie = cookieOpt.get.cookie
          Get("third") ~> addHeader(Cookie(cookie)) ~> sessionRoute ~> check {
            responseAs[String] === "2"
          }
        }
      }
    }

    "not be accessible for invalid session identifiers" in new SessionApp {
      Get("invalid") ~> addHeader(Cookie(HttpCookie(name = manager.cookieName, content = "%invalid-session-id%"))) ~> sealRoute(sessionRoute) ~> check {
        status === Unauthorized
        responseAs[String] === "Unknown session %invalid-session-id%"
      }
    }

    "be deleted when session was invalidated" in new SessionApp {
      Get("create") ~> sessionRoute ~> check {
        responseAs[String] === "0"
        val cookieOpt = header[`Set-Cookie`]
        cookieOpt should beSome
        val cookie = cookieOpt.get.cookie
        Delete("delete") ~> addHeader(Cookie(cookie)) ~> sessionRoute ~> check {
          responseAs[String] === "ok"
          Get("invalid") ~> addHeader(Cookie(cookie)) ~> sealRoute(sessionRoute) ~> check {
            status === Unauthorized
            responseAs[String] === s"Unknown session ${cookie.content}"
          }
        }
      }
    }

    "be deleted after session timeout" in new SessionApp {
      Get("new") ~> sessionRoute ~> check {
        responseAs[String] === "0"
        val cookieOpt = header[`Set-Cookie`]
        cookieOpt should beSome
        val cookie = cookieOpt.get.cookie
        Thread.sleep(6000)
        Get("timedout") ~> addHeader(Cookie(cookie)) ~> sealRoute(sessionRoute) ~> check {
          status === Unauthorized
          responseAs[String] === s"Unknown session ${cookie.content}"
        }
      }
    }

  }

}
