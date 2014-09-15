package spray
package routing
package session

import com.typesafe.config.ConfigFactory

class InMemorySessionSpec extends StatefulSessionSpec {

  def manager = new InMemorySessionManager[Int](ConfigFactory.load())

}
