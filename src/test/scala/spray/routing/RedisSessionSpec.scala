package spray
package routing

import session.RedisSessionManager

import com.typesafe.config.ConfigFactory

import spray.json.DefaultJsonProtocol._

class RedisSessionSpec extends SessionSpec {

  skipAllIf(true)

  def manager = new RedisSessionManager[Int](ConfigFactory.load())

}
