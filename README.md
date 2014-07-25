Spray Session [![Build Status](https://travis-ci.org/gnieh/spray-session.png?branch=master)](https://travis-ci.org/gnieh/spray-session)
=============

Provide session management for [Spray](http://spray.io/) applications.
There are three session managers implemented:
 - In memory sessions (stateful session manager),
 - Session saved in a [redis](http://redis.io/) server (stateful session manager, optional dependency on [redisreact](https://github.com/debasishg/scala-redis-nb/)),
 - Cookie baked sessions (stateless session manager).

One can easily add new session managers by implementing the trait `spray.routing.session.StatefulSessionManager` or `spray.routing.session.StatelessSessionManager`
depending on .

To get access to the session directives you must extends either the trait `spray.routing.directives.StatefulSessionDirectives` or `spray.routing.directives.StatelessSessionDirectives` and provide an implementation of the session manager.

For an example of session management, please refer to the tests.
