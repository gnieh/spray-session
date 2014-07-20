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
package directives

import session.StatelessSessionManager

import http.HttpCookie

import scala.concurrent.ExecutionContext

import com.typesafe.config.ConfigFactory

import shapeless._

/** Provides directives that give access to stateless session management.
 *  Stateless means that the session state can completely be extracted from the cookie
 *  and needs no server-side storing.
 *  It is retrieved from a cookie which name can be configured using the
 *  `spray.routing.session.cookie-name` configuration key.
 *
 *  @author Lucas Satabin
 */
trait StatelessSessionDirectives[T] extends BasicDirectives with CookieDirectives with FutureDirectives with RouteDirectives {

  def manager: StatelessSessionManager[T]

  implicit val ec: ExecutionContext

  private val config =
    ConfigFactory.load()

  private val cookieName =
    config.getString("spray.routing.session.cookie-name")

  /** Returns the session data extracted from the cookie if it is valid */
  def session(cookie: HttpCookie): Directive1[Option[Map[String, T]]] =
    onSuccess(manager.get(cookie))

  /** Creates a new empty session returns it */
  def newSession(): Directive1[Map[String, T]] =
    onSuccess(manager.start())

  /** Sets an invalidated session cookie */
  def invalidate(): Directive0 =
    onSuccess(manager.invalidate()).hflatMap {
      case cookie :: HNil => setCookie(cookie)
    }

  /** Gets the current session given by the cookie if any.
   *  If no session cookie exists, a new session is started and returned.
   *  If an invalid session cookie is given, the request is rejected */
  def cookieSession: Directive1[Map[String, T]] =
    optionalCookie(cookieName).hflatMap {
      case Some(cookie) :: HNil =>
        session(cookie).hflatMap {
          case Some(sess) :: HNil =>
            hprovide(sess :: HNil)
          case None :: HNil =>
            // the session does not exist or has expired, reject
            reject(InvalidSessionRejection(cookie.content))
        }

      case None :: HNil =>
        newSession
    }

  /** Sets the cookie session to send back to the client */
  def setCookieSession(map: Map[String, T]): Directive0 =
    onSuccess(manager.cookify(map)).hflatMap {
      case cookie :: HNil => setCookie(cookie)
    }

}
