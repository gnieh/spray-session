name := "spray-session"

organization := "org.gnieh"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.11.2", "2.10.4")

libraryDependencies += "io.spray" %% "spray-json" % "1.2.6" % "optional"

libraryDependencies += "io.spray" %% "spray-routing" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-testkit" % "1.3.1" % "test"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.13" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.5" % "provided, test"

libraryDependencies += "net.debasishg" %% "redisreact" % "0.7" % "optional"

scalacOptions ++= Seq("-deprecation", "-feature")

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
