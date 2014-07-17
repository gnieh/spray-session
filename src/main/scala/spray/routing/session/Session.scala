package spray
package routing
package session

import http.DateTime

private case class Session(map: Map[String, Any], expires: Option[DateTime])
