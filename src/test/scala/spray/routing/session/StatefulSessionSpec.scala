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

abstract class StatefulSessionSpec extends Specification with Specs2RouteTest {
  self =>

  implicit val timeout = new Timeout(Duration(20, SECONDS))

  implicit def actorRefFactory = ActorSystem()

  def manager: StatefulSessionManager[Int]

  trait StatefulSessionApp extends HttpService with StatefulSessionManagerDirectives[Int] with Scope with After {

    lazy val actorRefFactory = system

    val invalidSessionHandler = RejectionHandler {
      case InvalidSessionRejection(id) :: _ =>
        complete((Unauthorized, s"Unknown session $id"))
    }

    // create a new manager for each scope
    implicit val manager = self.manager

    def after = actorRefFactory.shutdown()

    val sessionRoute =
      handleRejections(invalidSessionHandler) {
          cookieSession() { (id, map) =>
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
          cookieOpt should beNone
          Get("third") ~> addHeader(Cookie(cookie)) ~> sessionRoute ~> check {
            responseAs[String] === "2"
          }
        }
      }
    }

    "be renewed for invalid session identifiers" in new StatefulSessionApp {
      Get("invalid") ~> addHeader(Cookie(HttpCookie(name = manager.cookieName, content = "%invalid-session-id%"))) ~> sealRoute(sessionRoute) ~> check {
        responseAs[String] === "0"
      }
    }

    "be deleted when session was invalidated" in new StatefulSessionApp {
      Get("create") ~> sessionRoute ~> check {
        responseAs[String] === "0"
        val cookieOpt = header[`Set-Cookie`]
        cookieOpt should beSome
        val cookie = cookieOpt.get.cookie
        Delete("delete") ~> addHeader(Cookie(cookie)) ~> sessionRoute ~> check {
          responseAs[String] === "ok"
          Get("invalid") ~> addHeader(Cookie(cookie)) ~> sealRoute(sessionRoute) ~> check {
            responseAs[String] === s"0"
          }
        }
      }
    }

    "be deleted after session timeout" in new StatefulSessionApp {
      Get("new") ~> sessionRoute ~> check {
        responseAs[String] === "0"
        val cookieOpt = header[`Set-Cookie`]
        cookieOpt should beSome
        val cookie = cookieOpt.get.cookie
        Thread.sleep(6000)
        Get("timedout") ~> addHeader(Cookie(cookie)) ~> sealRoute(sessionRoute) ~> check {
          responseAs[String] === s"0"
        }
      }
    }

  }

}
