import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

sbtPlugin := true

name := "openmole-buildsystem-plugin"

organization := "org.openmole"

Global / resolvers += Resolver.sbtPluginRepo("releases")
Global / resolvers += Resolver.sonatypeRepo("staging")

addSbtPlugin("com.github.sbt" % "sbt-osgi" % "0.10.0")
addSbtPlugin("org.openmole" % "sbt-osgi" % "0.9.16-SNAPSHOT")

libraryDependencies ++= Seq(
  "com.jsuereth" %% "scala-arm" % "2.0",
  "org.apache.commons" % "commons-compress" % "1.24.0",
  //"com.github.sbt" % "sbt-osgi_2.12_1.0" % "0.10.0",
  "org.json4s" %% "json4s-native" % "3.5.3")
