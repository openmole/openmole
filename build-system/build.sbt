sbtPlugin := true

name := "openmole-buildsystem-plugin"

organization := "org.openmole"

version := "0.9-SNAPSHOT"

resolvers += Classpaths.sbtPluginSnapshots

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.6.0-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.1")

libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.3"

publishTo <<= isSnapshot(if(_) Some("Openmole Nexus" at "https://maven.openmole.org/snapshots") else Some("Openmole Nexus" at "https://maven.openmole.org/releases"))

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

