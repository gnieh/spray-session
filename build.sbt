name := "spray-session"

organization := "org.gnieh"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.4"

//crossScalaVersions := Seq("2.11.2", "2.10.4")

libraryDependencies += "io.spray" %% "spray-json" % "1.2.6" % "optional"

libraryDependencies += "io.spray" % "spray-routing" % "1.3.1"

libraryDependencies += "io.spray" % "spray-testkit" % "1.3.1" % "test"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.13" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.5" % "provided, test"

libraryDependencies += "net.debasishg" %% "redisreact" % "0.6" % "optional"

scalacOptions ++= Seq("-deprecation", "-feature")
