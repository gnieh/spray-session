Spray Session [![Build Status](https://travis-ci.org/gnieh/spray-session.png?branch=master)](https://travis-ci.org/gnieh/spray-session)
=============

Provide session management for [Spray](http://spray.io/) applications.
There are three session managers implemented:
 - In memory sessions (stateful session manager),
 - Session saved in a [redis](http://redis.io/) server (stateful session manager, optional dependency on [redisreact](https://github.com/debasishg/scala-redis-nb/)),
 - Cookie baked sessions (stateless session manager).

One can easily add new session managers by implementing the trait `spray.routing.session.StatefulSessionManager` or `spray.routing.session.StatelessSessionManager`
depending on .

To get access to the session directives you can extend either the trait `spray.routing.directives.StatefulSessionDirectives` or `spray.routing.directives.StatelessSessionDirectives`.

Sample Code
-----------

Below are two code snippets showing how the session directives can be used.
The first one demonstrates the use of a stateful session manager, and the second one how to use a stateless session manager.

```scala
import spray.routing._
import directives._
import session._

import akka.actor.ActorSystem
import akka.util.Timeout

import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory

class MyService(implicit val actorRefFactory: ActorSystem) extends HttpService with StatefulSessionManagerDirectives[Int] {

  val config = ConfigFactory.load()

  import actorRefFactory.dispatcher

  implicit val timeout = Timeout(20.seconds)

  implicit val manager = new InMemorySessionManager[Int](config)

  val route =
    withCookieSession() { (id, map) =>
      get {
        val result = map.getOrElse("value", 0)
        updateSession(id, map.updated("value", result + 1)) {
            complete(result.toString)
          }
      } ~
      delete {
        invalidateSession(id) {
          complete("ok")
        }
      }
    }
}
```

~~~~~~~~

```scala
import spray.routing._
import directives._
import session._

import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory

class MyService(implicit val actorRefFactory: ActorSystem) extends HttpService with StatelessSessionManagerDirectives[String] {

  val config = ConfigFactory.load()

  import actorRefFactory.dispatcher

  implicit val manager = new CookieBakerSessionManager(config)

  val route =
    cookieSession() { map =>
      get {
        val result = map.getOrElse("value", "0")
        setCookieSession(map.updated("value", (result.toInt + 1).toString)) {
          complete(result)
        }
      } ~
      delete {
        invalidate() {
          complete("ok")
        }
      }
    }
}
```
