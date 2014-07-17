package spray
package routing

import session.InMemorySessionManager

import com.typesafe.config.ConfigFactory

class InMemorySessionSpec extends SessionSpec {

  def manager = new InMemorySessionManager[Int](ConfigFactory.load())

}
