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

/** Common interface for stateless session managers, making it transparent on how
 *  they are stored and persisted.
 *  Stateless means that the entire session data can be retrieved from the cookie.
 *
 *  @author Lucas Satabin
 */
abstract class StatelessSessionManager[T](val config: Config) {

  /** The name of the session cookie, configured via configuration key `spray.routing.session.cookie-name` */
  val cookieName: String =
    config.getString("spray.routing.session.cookie-name")

  /** The duration of a session, configured via configuration key `spray.routing.session.timeout` */
  val sessionTimeout: Duration =
    new PimpedConfig(config).getDuration("spray.routing.session.timeout")

  /** Starts a new session with a new identifier */
  def start(): Future[HttpCookie]

  /** Checks whether the identifier is a valid session cookie */
  def isValid(cookie: HttpCookie): Future[Boolean]

  /** Returns the session identified by `cookie` if it exists and is valid */
  def get(cookie: HttpCookie): Future[Option[Map[String, T]]]

  /** Updates the session identified with the given session value */
  def update(map: Map[String, T]): Future[HttpCookie]

  /** Invalidates the given session identified by the given identifier */
  def invalidate(): Future[HttpCookie]

}
