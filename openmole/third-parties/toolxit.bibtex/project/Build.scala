import sbt._
import Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "toolxit.bibtex",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.6",
    crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.10.5", "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4",
      "2.11.5", "2.11.6"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq("-deprecation")
  )
}

object MyBuild extends Build {
  import BuildSettings._

  lazy val root: Project = Project(
    "root",
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in core)
  ) aggregate(macros, core)

  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
      libraryDependencies := {
        CrossVersion.partialVersion(scalaVersion.value) match {
          // if Scala 2.11+ is used, quasiquotes are available in the standard distribution
          case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          libraryDependencies.value
          // in Scala 2.10, quasiquotes are provided by macro paradise
          case Some((2, 10)) =>
          libraryDependencies.value ++ Seq(
            compilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full),
            "org.scalamacros" %% "quasiquotes" % "2.1.0-M5" cross CrossVersion.binary)
        }
      }
    )
  )

  lazy val core: Project = Project(
    "core",
    file("core"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq (
        "org.freemarker" % "freemarker" % "2.3.19",
        "junit" % "junit" % "4.10" % "test",
        "org.scalatest" % s"scalatest" % "2.2.4" % "test" cross CrossVersion.binary
      ),
      libraryDependencies := {
        CrossVersion.partialVersion(scalaVersion.value) match {
          // if Scala 2.11+ is used, the now external xml and parser combinators shall be added
          case Some((2, scalaMajor)) if scalaMajor >= 11 =>
            libraryDependencies.value ++ Seq(
              "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
              "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3"
            )
          // in Scala 2.10, xml and parser combinators are shipped with the library
          case Some((2, 10)) =>
            libraryDependencies.value
        }
      }
    )
  ) dependsOn(macros)
}
