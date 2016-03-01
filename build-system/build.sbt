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

resolvers += Classpaths.sbtPluginSnapshots

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.8.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.6.0")


libraryDependencies ++= Seq(
  "com.jsuereth" %% "scala-arm" % "1.3",
  "org.apache.commons" % "commons-compress" % "1.10")


publishTo <<= isSnapshot(if(_) Some("Openmole Nexus" at "https://maven.openmole.org/snapshots") else Some("Openmole Nexus" at "https://maven.openmole.org/releases"))

releaseSettings
