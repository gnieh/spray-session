/*
* Copyright © 2014 spray-session
* Based on Play2.0 CookieBaker code.
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

import scala.concurrent.{
  Future,
  ExecutionContext
}
import scala.concurrent.duration.Duration

import http.HttpCookie

import util._

import com.typesafe.config.Config

import java.net.{
  URLEncoder,
  URLDecoder
}

/** Cookie baker code ported from Play2.0.
 *  Cookies can be signed by setting `spray.routing.session.baker.signed` to `true`.
 *  In this case one must configure the secret key by setting the
 *  `spray.routing.session.baker.secret` configuration key.
 *
 *  @author Lucas Satabin
 */
class CookieBakerSessionManager(config: Config)(implicit ec: ExecutionContext) extends StatelessSessionManager[String](config) {

  private lazy val secret =
    Option(config.getString("spray.routing.session.baker.secret"))
      .fold(
        throw new CryptoException("Configuration error: missing `spray.routing.session.baker.secret`")
      )(_.getBytes("utf-8"))

  def get(cookie: HttpCookie): Future[Option[Map[String,String]]] =
    Future(decode(cookie.content))

  def invalidate(): Future[HttpCookie] =
    Future.successful(HttpCookie(name = cookieName, content = "", maxAge = Some(-1)))

  def isValid(cookie: HttpCookie): Future[Boolean] = ???

  def start(): Future[Map[String, String]] =
    Future.successful(Map())

  def cookify(map: Map[String,String]): Future[HttpCookie] =
   Future(HttpCookie(name = cookieName, content = encode(map), maxAge = maxAge, path = cookiePath, domain = cookieDomain, secure = cookieSecure, httpOnly = cookieHttpOnly))

  val isSigned: Boolean =
    config.getBoolean("spray.routing.session.baker.signed")

  val maxAge: Option[Long] = {
    val duration = config.getDuration("spray.routing.session.timeout")
    if(duration.isFinite)
      Some(duration.toSeconds)
    else
      None
  }

  /** Encodes the data as a `String`. */
  def encode(data: Map[String, String]): String = {
    val encoded =
      URLEncoder.encode(
        data
          .filterNot(_._1.contains(":"))
          .map(d => d._1 + ":" + d._2)
          .mkString("\u0000"), "UTF-8")

    if(isSigned)
      Crypto.mac(encoded, secret) + "-" + encoded
    else
      encoded
  }

  /** Decodes from an encoded `String`. */
  def decode(data: String): Option[Map[String, String]] = {

    def urldecode(data: String) =
      Some(URLDecoder.decode(data, "UTF-8")
        .split("\u0000")
        .map(_.split(":"))
        .map(p => p(0) -> p.drop(1).mkString(":"))
        .toMap)


    // Do not change this unless you understand the security issues behind timing attacks.
    // This method intentionally runs in constant time if the two strings have the same length.
    // If it didn't, it would be vulnerable to a timing attack.
    def safeEquals(a: String, b: String) =
      if (a.length != b.length) {
        false
      } else {
        var equal = 0
        for (i <- Array.range(0, a.length)) {
          equal |= a(i) ^ b(i)
        }
        equal == 0
      }

    try {
      if (isSigned) {
        val splitted = data.split("-")
        val message = splitted.tail.mkString("-")
        if (safeEquals(splitted(0), Crypto.mac(message, secret))) {
          urldecode(message)
        }
        else {
          Some(Map.empty[String, String])
        }
      } else urldecode(data)
    } catch {
      // fail gracefully if the session cookie is corrupted
      case _: Exception => None
    }
  }

}
