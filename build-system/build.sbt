import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

sbtPlugin := true

name := "openmole-buildsystem-plugin"

organization := "org.openmole"

Global / resolvers += Resolver.sbtPluginRepo("releases")
Global / resolvers += Resolver.sonatypeRepo("staging")

addSbtPlugin("org.openmole" % "sbt-osgi" % "0.9.10")

libraryDependencies ++= Seq(
  "com.jsuereth" %% "scala-arm" % "2.0",
  "org.apache.commons" % "commons-compress" % "1.10",
  "org.json4s" %% "json4s-native" % "3.5.3")
