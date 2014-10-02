/*
* Copyright © 2014 spray-session
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package spray
package routing
package session
package directives

import scala.concurrent.{
  Future,
  ExecutionContext
}

import spray.routing.directives.{
  BasicDirectives,
  CookieDirectives,
  RouteDirectives,
  FutureDirectives
}

import shapeless._

import scala.language.implicitConversions

/** Provides directives that give access to stateful session management.
 *  Stateful means that the session states is saved on the server side.
 *  It is retrieved from a cookie which name can be configured using the
 *  `spray.routing.session.cookie-name` configuration key.
 *
 *  @author Lucas Satabin
 */
trait StatefulSessionManagerDirectives[T] extends BasicDirectives with CookieDirectives with RouteDirectives {

  /** Returns the session with the given identifier if it exists and has not expired */
  def session(magnet: WithStatefulManagerMagnet[String, T]): Directive1[Option[Map[String, T]]] =
    magnet.directive(_.get(magnet.in))

  /** Creates a new session and returns its identifier */
  def newSession(magnet: WithStatefulManagerMagnet[Unit, T]): Directive1[String] =
    magnet.directive(_.start())

  /** Updates the current session map with the given (key, value) association */
  def updateSession(magnet: WithStatefulManagerMagnet[(String, Map[String, T]), T]): Directive0 =
    magnet.directive(_.update(magnet.in._1, magnet.in._2)).hflatMap(_ => pass)

  /** Invalidates the given session identifier */
  def invalidateSession(magnet: WithStatefulManagerMagnet[String, T]): Directive0 =
    magnet.directive(_.invalidate(magnet.in)).hflatMap(_ => pass)

  /** Gets the current session givven by the cookie if any.
   *  If no session cookie exists, a new session is started and returned.
   *  If an invalid or expired session identifier is given, the request is rejected */
  def cookieSession(magnet: WithStatefulManagerMagnet[Unit, T]): Directive[String :: Map[String, T] :: HNil] =
    optionalCookie(magnet.manager.cookieName).hflatMap {
      case Some(cookie) :: HNil =>
        magnet.directive(_.get(cookie.content)).hflatMap {
          case Some(sess) :: HNil =>
            hprovide(cookie.content :: sess :: HNil)
          case None :: HNil =>
            // the session does not exist or has expired, reject
            // just start a new one and discard old cookie
            deleteCookie(cookie).hflatMap {
              case HNil =>
                startFresh(magnet)
            }
        }

      case None :: HNil =>
        startFresh(magnet)

    }

  private def startFresh(magnet: WithStatefulManagerMagnet[Unit, T]): Directive[String :: Map[String, T] :: HNil] =
      magnet.directive(_.start()).hflatMap {
        case id :: HNil =>
          magnet.directive(_.get(id)).hflatMap {
            case Some(map) :: HNil =>
              magnet.directive(_.cookify(id)).hflatMap {
                case cookie :: HNil =>
                  setCookie(cookie).hmap { _ =>
                    id :: map :: HNil
                  }
              }
            case None :: HNil =>
              // actually, this case should never happen if we configured a meaningful
              // timeout (merely meaning, not so ridiculously small, that the session
              // already timed out between the time it was created just above and now)
              reject(InvalidSessionRejection(id))
          }
      }

  /** Sets the cookie session to send back to the client */
  def setCookieSession(magnet: WithStatefulManagerMagnet[String, T]): Directive0 =
    magnet.directive(_.cookify(magnet.in)).hflatMap {
      case cookie :: HNil => setCookie(cookie)
    }

}

trait WithStatefulManagerMagnet[In,T] {
  import FutureDirectives._

  implicit val executor: ExecutionContext

  implicit val manager: StatefulSessionManager[T]

  val in: In

  def directive[Out](action: StatefulSessionManager[T] => Future[Out]): Directive1[Out] =
    onSuccess(action(manager))

}

object WithStatefulManagerMagnet {

  implicit def apply[In,T](i: In)(implicit ec: ExecutionContext,
    m: StatefulSessionManager[T]): WithStatefulManagerMagnet[In,T] =
    new WithStatefulManagerMagnet[In,T] {
      implicit val executor = ec
      val manager = m
      val in = i
    }

}
