package spray.routing.session

import com.typesafe.config.Config
import spray.http.HttpCookie

import scala.concurrent.Future

/**
 * Common session cookie trait used in both stateful and stateless session managers
 */
trait CookieManager[T] {

    /* needs a typesafe config to get the settings from */
    def config : Config

    /** The name of the session cookie, configured via configuration key `spray.routing.session.cookie.name` */
    val cookieName: String =
        config.getString("spray.routing.session.cookie.name")

    /** The domain of the session cookie, configured via configuration key `spray.routing.session.cookie.domain` */
    val cookieDomain: Option[String] =
        config.getString("spray.routing.session.cookie.domain") match {
            case s : String if !s.isEmpty()  => Some(s)
            case _ => None
        }

    /** The path of the session cookie, configured via configuration key `spray.routing.session.cookie.path` */
    val cookiePath: Option[String] =
        config.getString("spray.routing.session.cookie.path") match {
            case s : String if !s.isEmpty()  => Some(s)
            case _ => None
        }

    /** Whether or not the session cookie will be secure, configured via configuration key `spray.routing.session.cookie.secure` */
    val cookieSecure: Boolean =
        config.getBoolean("spray.routing.session.cookie.secure")

    /** Whether or not the session cookie will be http only, configured via configuration key `spray.routing.session.cookie.httpOnly` */
    val cookieHttpOnly: Boolean =
        config.getBoolean("spray.routing.session.cookie.httpOnly")

    /** Returns the cookie value for the given session identifier */
    def cookify(payload : T): Future[HttpCookie]
}
