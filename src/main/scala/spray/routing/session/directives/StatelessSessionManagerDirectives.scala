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

import spray.http.HttpCookie

import spray.routing.directives.{
  BasicDirectives,
  CookieDirectives,
  RouteDirectives,
  FutureDirectives
}

import scala.concurrent.{
  Future,
  ExecutionContext
}

import shapeless._

import scala.language.implicitConversions

/** Provides directives that give access to stateless session management.
 *  Stateless means that the session state can completely be extracted from the cookie
 *  and needs no server-side storing.
 *  It is retrieved from a cookie which name can be configured using the
 *  `spray.routing.session.cookie-name` configuration key.
 *
 *  @author Lucas Satabin
 */
trait StatelessSessionManagerDirectives[T] extends BasicDirectives with CookieDirectives with RouteDirectives {

  /** Returns the session data extracted from the cookie if it is valid */
  def session(magnet: WithStatelessManagerMagnet[HttpCookie,T]): Directive1[Option[Map[String, T]]] =
    magnet.directive(_.get(magnet.in))

  /** Creates a new empty session returns it */
  def newSession(magnet: WithStatelessManagerMagnet[Unit,T]): Directive1[Map[String, T]] =
    magnet.directive(_.start())

  /** Sets an invalidated session cookie */
  def invalidate(magnet: WithStatelessManagerMagnet[Unit,T]): Directive0 =
    magnet.directive(_.invalidate()).hflatMap {
      case cookie :: HNil => setCookie(cookie)
    }

  /** Gets the current session given by the cookie if any.
   *  If no session cookie exists, a new session is started and returned.
   *  If an invalid session cookie is given, the request is rejected */
  def cookieSession(magnet: WithStatelessManagerMagnet[Unit,T]): Directive1[Map[String, T]] =
    optionalCookie(magnet.manager.cookieName).hflatMap {
      case Some(cookie) :: HNil =>
        magnet.directive(_.get(cookie)).hflatMap {
          case Some(sess) :: HNil =>
            hprovide(sess :: HNil)
          case None :: HNil =>
            // the session does not exist or has expired, reject
            reject(InvalidSessionRejection(cookie.content))
        }

      case None :: HNil =>
        magnet.directive(_.start())
    }

  /** Sets the cookie session to send back to the client */
  def setCookieSession(magnet: WithStatelessManagerMagnet[Map[String, T], T]): Directive0 =
    magnet.directive(_.cookify(magnet.in)).hflatMap {
      case cookie :: HNil => setCookie(cookie)
    }

}

trait WithStatelessManagerMagnet[In,T] {
  import FutureDirectives._

  implicit val executor: ExecutionContext

  implicit val manager: StatelessSessionManager[T]

  val in: In

  def directive[Out](action: StatelessSessionManager[T] => Future[Out]): Directive1[Out] =
    onSuccess(action(manager))

}

object WithStatelessManagerMagnet {

  implicit def apply[In,T](i: In)(implicit ec: ExecutionContext,
    m: StatelessSessionManager[T]): WithStatelessManagerMagnet[In,T] =
    new WithStatelessManagerMagnet[In,T] {
      implicit val executor = ec
      val manager = m
      val in = i
    }

}
