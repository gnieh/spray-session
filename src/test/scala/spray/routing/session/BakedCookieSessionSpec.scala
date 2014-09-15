package spray
package routing
package session

import com.typesafe.config.ConfigFactory

class BakedCookieSessionSpec extends StatelessSessionSpec {

  def manager = new CookieBakerSessionManager(ConfigFactory.load())

}
