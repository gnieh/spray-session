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

import session.SessionManager

import scala.concurrent.ExecutionContext

import com.typesafe.config.ConfigFactory

import shapeless._

/** Provides directives that give access to session management.
 *  It also provides directive to retrieve the session identifier from
 *  a cookie.
 *  In the case the `cookieSession` directive is used, an empty map session is returned
 *  if it does not exist yet, but no session is started.
 *
 *  @author Lucas Satabin
 */
trait SessionDirectives[T] extends BasicDirectives with CookieDirectives with FutureDirectives with RouteDirectives {

  def manager: SessionManager[T]

  implicit val ec: ExecutionContext

  private val config =
    ConfigFactory.load()

  private val cookieName =
    config.getString("spray.routing.session.cookie-name")

  /** Returns the session with the given identifier if it exists and has not expired */
  def session(id: String): Directive1[Option[Map[String, T]]] =
    onSuccess(manager.get(id))

  /** Creates a new session and returns its identifier */
  def newSession(): Directive1[String] =
    onSuccess(manager.start())

  /** Updates the current session map with the given (key, value) association */
  def updateSession(id: String, map: Map[String, T]): Directive0 =
    onSuccess(manager.update(id, map)).hflatMap(_ => pass)

  /** Invalidates the given session identifier */
  def invalidateSession(id: String): Directive0 =
    onSuccess(manager.invalidate(id)).hflatMap(_ => pass)

  /** Gets the current session givven by the cookie if any.
   *  If no session cookie exists, a new session is started and returned.
   *  If an invalid or expired session identifier is given, the request is rejected */
  def cookieSession: Directive[String :: Map[String, T] :: HNil] =
    optionalCookie(cookieName).hflatMap {
      case Some(cookie) :: HNil =>
        session(cookie.content).hflatMap {
          case Some(sess) :: HNil =>
            hprovide(cookie.content :: sess :: HNil)
          case None :: HNil =>
            // the session does not exist or has expired, reject
            reject(InvalidSessionRejection(cookie.content))
        }

      case None :: HNil =>
        newSession.hflatMap {
          case id :: HNil =>
            session(id).hflatMap {
              case Some(map) :: HNil =>
                hprovide(id :: map :: HNil)
              case None :: HNil =>
                // actually, this case should never happen if we configured a meaningful
                // timeout (merely meaning, not so ridiculously small, that the session
                // already timed out between the time it was created just above and now)
                reject(InvalidSessionRejection(id))
            }
        }

    }

  /** Sets the cookie session to send back to the client */
  def setCookieSession(id: String): Directive0 =
    onSuccess(manager.cookify(id)).hflatMap {
      case cookie :: HNil => setCookie(cookie)
    }

}
