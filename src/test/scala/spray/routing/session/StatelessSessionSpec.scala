package spray
package routing
package session

import org.specs2.mutable.{
  Specification,
  After
}
import org.specs2.specification.Scope

import testkit.Specs2RouteTest

import directives._

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

abstract class StatelessSessionSpec extends Specification with Specs2RouteTest {
  self =>

  implicit val timeout = new Timeout(Duration(20, SECONDS))

  implicit lazy val actorRefFactory = ActorSystem()

  def manager: StatelessSessionManager[String]

  trait StatefulSessionApp extends HttpService with StatelessSessionManagerDirectives[String] with Scope with After {

    def actorRefFactory = system

    val invalidSessionHandler = RejectionHandler {
      case InvalidSessionRejection(id) :: _ =>
        complete(Unauthorized, s"Unknown session $id")
    }

    // create a new manager for each scope
    implicit val manager = self.manager

    def after = actorRefFactory.shutdown()

    val sessionRoute =
      handleRejections(invalidSessionHandler) {
          cookieSession() { map =>
            get {
              val result = map.getOrElse("value", "0")
              setCookieSession(map.updated("value", (result.toInt + 1).toString)) {
                complete(result)
              }
            } ~
            delete {
              invalidate() {
                complete("ok")
              }
            }
          }
      }

  }

  "Session state" should {

    "be created when no cookie is sent" in new StatefulSessionApp {
      Get("new") ~> sessionRoute ~> check {
        responseAs[String] === "0"
      }
    }

    "be kept between two request with same session id" in new StatefulSessionApp {
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

    "not be accessible for invalid session identifiers" in new StatefulSessionApp {
      Get("invalid") ~> addHeader(Cookie(HttpCookie(name = manager.cookieName, content = "%invalid-session-id%"))) ~> sealRoute(sessionRoute) ~> check {
        status === Unauthorized
        responseAs[String] === "Unknown session %invalid-session-id%"
      }
    }

  }

}
