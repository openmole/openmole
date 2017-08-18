import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

scalariformSettings(autoformat = true)

ScalariformKeys.preferences := ScalariformKeys.preferences (p =>
  p.setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(CompactControlReadability, true)).value

scalacOptions += "-optimize"

sbtPlugin := true

name := "openmole-buildsystem-plugin"

organization := "org.openmole"

//resolvers += Classpaths.sbtPluginSnapshots

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.1")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")


libraryDependencies ++= Seq(
  "com.jsuereth" %% "scala-arm" % "1.3",
  "org.apache.commons" % "commons-compress" % "1.10")

