osgiSettings

OsgiKeys.exportPackage := Seq(
  "spray.routing.session.directives",
  "spray.routing.session"
)

OsgiKeys.importPackage := Seq(
  "com.redis;resolution:=optional",
  "com.redis.*;resolution:=optional",
  "*"
)

OsgiKeys.additionalHeaders := Map (
  "Bundle-Name" -> "Session Management for Spray"
)

OsgiKeys.bundleSymbolicName := "org.gnieh.spray.session"
OsgiKeys.privatePackage := Seq()
