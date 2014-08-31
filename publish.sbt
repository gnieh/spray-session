publishMavenStyle := true

publishArtifact in Test := false

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { x => false }

pomExtra := (
  <scm>
    <url>https://github.com/gnieh/spray-session</url>
    <connection>scm:git:git://github.com/gnieh/spray-session.git</connection>
    <developerConnection>scm:git:git@github.com:gnieh/spray-session.git</developerConnection>
    <tag>HEAD</tag>
  </scm>
  <developers>
    <developer>
      <id>satabin</id>
      <name>Lucas Satabin</name>
      <email>lucas.satabin@gnieh.org</email>
    </developer>
  </developers>
  <ciManagement>
    <system>travis</system>
    <url>https://travis-ci.org/#!/gnieh/spray-session</url>
  </ciManagement>
  <issueManagement>
    <system>github</system>
    <url>https://github.com/gnieh/spray-session/issues</url>
  </issueManagement>
)
