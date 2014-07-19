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

import scala.concurrent.Future
import scala.concurrent.duration.Duration

import http.HttpCookie

import util.pimps.PimpedConfig

import com.typesafe.config.Config

/** Common interface for stateful session managers, making it transparent on how
 *  they are stored and persisted.
 *
 *  @author Lucas Satabin
 */
abstract class StatefulSessionManager[T](val config: Config) {

  private val alpha = "abcdefghijklmnopqrstuvwxyz"
  private val symbols = alpha + alpha.toUpperCase + "0123456789/=?+-_:"
  private val symLength = symbols.length
  private val idLength = 16
  private val random = new java.security.SecureRandom

  /** The name of the session cookie, configured via configuration key `spray.routing.session.cookie-name` */
  val cookieName: String =
    config.getString("spray.routing.session.cookie-name")

  /** The duration of a session, configured via configuration key `spray.routing.session.timeout` */
  val sessionTimeout: Duration =
    new PimpedConfig(config).getDuration("spray.routing.session.timeout")

  /** Generates a new identifier */
  protected def newSid(): String = {
    val buf = new StringBuilder
    (1 to idLength).foreach(_ => buf.append(symbols.charAt(random.nextInt(symLength))))
    buf.toString
  }

  /** Starts a new session with a new identifier */
  def start(): Future[String]

  /** Checks whether the identifier is a valid session identifier */
  def isValid(id: String): Future[Boolean]

  /** Returns the session identified by `id`if it exists and is valid */
  def get(id: String): Future[Option[Map[String, T]]]

  /** Updates the session identified by the given identifier with the given session value */
  def update(id: String, map: Map[String, T]): Future[Unit]

  /** Invalidates the given session identified by the given identifier */
  def invalidate(id: String): Future[Unit]

  /** Returns the cookie value for the given session identifier */
  def cookify(id: String): Future[HttpCookie]

  /** Shut this session manager down */
  def shutdown(): Unit

}
