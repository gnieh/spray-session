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

class InMemorySessionSpec extends SessionSpec {

  val manager = new InMemorySessionManager[Int](ConfigFactory.load())

}
