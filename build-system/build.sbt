sbtPlugin := true

name := "openmole-buildsystem-plugin"

organization := "org.openmole"

resolvers += Classpaths.sbtPluginSnapshots

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.6.0-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.1")

libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.3"
