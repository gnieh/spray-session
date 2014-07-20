package spray
package routing

import session.CookieBakerSessionManager

import com.typesafe.config.ConfigFactory

class BakedCookieSessionSpec extends StatelessSessionSpec {

  def manager = new CookieBakerSessionManager(ConfigFactory.load())

}
