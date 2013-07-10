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

version := "0.9.14"

resolvers += Classpaths.sbtPluginSnapshots

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.5.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.1")

libraryDependencies ++= Seq("com.jsuereth" %% "scala-arm" % "1.3",
                            "org.kamranzafar" % "jtar" % "2.2")


publishTo <<= isSnapshot(if(_) Some("Openmole Nexus" at "http://maven.openmole.org/snapshots") else Some("Openmole Nexus" at "http://maven.openmole.org/releases"))

credentials += Credentials(Path.userHome / ".sbt" / "openmole.credentials")

releaseSettings
