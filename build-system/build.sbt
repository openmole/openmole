sbtPlugin := true

name := "openmole-buildsystem-plugin"

organization := "org.openmole"

resolvers += Classpaths.sbtPluginSnapshots

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.6.0-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.1")

libraryDependencies ++= Seq("com.jsuereth" %% "scala-arm" % "1.3",
                            "org.kamranzafar" % "jtar" % "2.2")


publishTo <<= isSnapshot(if(_) Some("Openmole Nexus" at "https://maven.openmole.org/snapshots") else Some("Openmole Nexus" at "https://maven.openmole.org/releases"))

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

