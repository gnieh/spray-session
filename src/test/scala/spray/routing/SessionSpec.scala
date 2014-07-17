package spray
package routing

import org.specs2.mutable.Specification

import testkit.Specs2RouteTest

import directives.SessionDirectives

import session.InMemorySessionManager

import http._
import HttpHeaders._
import StatusCodes._

import scala.concurrent.duration.{
  Duration,
  SECONDS
}
import scala.concurrent.Future

import akka.util.Timeout

import com.typesafe.config.ConfigFactory

abstract class SessionSpec extends Specification with Specs2RouteTest with HttpService with SessionDirectives[Int] {

  def actorRefFactory = system

  implicit val timeout = new Timeout(Duration(20, SECONDS))

  implicit val ec = system.dispatcher

  val invalidSessionHandler = RejectionHandler {
    case InvalidSessionRejection(id) :: _ =>
      complete(Unauthorized, s"Unknown session $id")
  }

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

  "Session state" should {

    "be created when no cookie is sent" in {
      Get() ~> sessionRoute ~> check {
        responseAs[String] === "0"
      }
    }

    "be kept between two request with same session id" in {
      Get() ~> sessionRoute ~> check {
        responseAs[String] === "0"
        val cookieOpt = header[`Set-Cookie`]
        cookieOpt should beSome
        val cookie = cookieOpt.get.cookie
        Get() ~> addHeader(Cookie(cookie)) ~> sessionRoute ~> check {
          responseAs[String] === "1"
          val cookieOpt = header[`Set-Cookie`]
          cookieOpt should beSome
          val cookie = cookieOpt.get.cookie
          Get() ~> addHeader(Cookie(cookie)) ~> sessionRoute ~> check {
            responseAs[String] === "2"
          }
        }
      }
    }

    "not be accessible for invalid session identifiers" in {
      Get() ~> addHeader(Cookie(HttpCookie(name = manager.cookieName, content = "%invalid-session-id%"))) ~> sealRoute(sessionRoute) ~> check {
        status === Unauthorized
        responseAs[String] === "Unknown session %invalid-session-id%"
      }
    }

    "be deleted when session was invalidated" in {
      Get() ~> sessionRoute ~> check {
        responseAs[String] === "0"
        val cookieOpt = header[`Set-Cookie`]
        cookieOpt should beSome
        val cookie = cookieOpt.get.cookie
        Delete() ~> addHeader(Cookie(cookie)) ~> sessionRoute ~> check {
          responseAs[String] === "ok"
          Get() ~> addHeader(Cookie(cookie)) ~> sealRoute(sessionRoute) ~> check {
            status === Unauthorized
            responseAs[String] === s"Unknown session ${cookie.content}"
          }
        }
      }
    }

    "be deleted after session timeout" in {
      Get() ~> sessionRoute ~> check {
        responseAs[String] === "0"
        val cookieOpt = header[`Set-Cookie`]
        cookieOpt should beSome
        val cookie = cookieOpt.get.cookie
        Thread.sleep(6000)
        Get() ~> addHeader(Cookie(cookie)) ~> sealRoute(sessionRoute) ~> check {
          status === Unauthorized
          responseAs[String] === s"Unknown session ${cookie.content}"
        }
      }
    }

  }

}
