import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences <<= ScalariformKeys.preferences (p =>
  p.setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(CompactControlReadability, true)
  .setPreference(PreserveDanglingCloseParenthesis, true))

scalacOptions += "-optimize"

sbtPlugin := true

name := "openmole-buildsystem-plugin"

organization := "org.openmole"

version := "1.4-SNAPSHOT"

resolvers += Classpaths.sbtPluginSnapshots

resolvers ++= Seq(DefaultMavenRepository,"openmole-public" at "http://maven.openmole.org/public")


addSbtPlugin("fr.iscpif" % "sbt-osgi" % "0.5.4-SNAPSHOT") //TODO: Get these changes mainlined

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.0")

libraryDependencies ++= Seq(
  "com.jsuereth" %% "scala-arm" % "1.3",
  "org.apache.commons" % "commons-compress" % "1.8.1")


publishTo <<= isSnapshot(if(_) Some("Openmole Nexus" at "http://maven.openmole.org/snapshots") else Some("Openmole Nexus" at "http://maven.openmole.org/releases"))

releaseSettings
