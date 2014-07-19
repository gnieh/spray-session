Spray Session [![Build Status](https://travis-ci.org/gnieh/spray-session.png?branch=master)](https://travis-ci.org/gnieh/spray-session)
=============

Provide session management for [Spray](http://spray.io/) applications.
There are three session managers implemented:
 - In memory sessions,
 - Cookie baked sessions,
 - Session saved in a [redis](http://redis.io/) server.

One can easily add new session managers by implementing the trait `spray.routing.session.SessionManager`.

To get access to the session directives you must extends the trait `spray.routing.session.directives.SessionDirectives` and provide an implementation of the session manager.

For an example of session management, please refer to the tests.
