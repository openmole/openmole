name := "scopt"

version := "1.1.3"

organization := "com.github.scopt"

licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

description := """a command line options parsing library"""

scalaVersion := "2.10.0-M7"

publishMavenStyle := true

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false

publishTo <<= (version) { version: String =>
  val nexus = "http://nexus.scala-tools.org/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus+"snapshots/") 
  else                                   Some("releases" at nexus+"releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers += ScalaToolsSnapshots

seq(lsSettings :_*)

LsKeys.tags in LsKeys.lsync := Seq("cli", "command-line", "parsing", "parser")
