name := "spray-session"
organization := "org.gnieh"
version := "0.1.0-SNAPSHOT"

crossScalaVersions := Seq("2.11.7", "2.10.6")
scalaVersion := crossScalaVersions.value.head

libraryDependencies ++= Seq(
  "io.spray" %% "spray-json" % "1.3.2" % "optional",
  "io.spray" %% "spray-routing" % "1.3.3",
  "io.spray" %% "spray-testkit" % "1.3.3" % "test",
  "org.specs2" %% "specs2-core" % "2.4.17" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.3.14" % "provided, test",
  "net.debasishg" %% "redisreact" % "0.8" % "optional"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xlint"
)
