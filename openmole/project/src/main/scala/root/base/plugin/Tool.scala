package root.base.plugin

import root.base._
import root.Libraries
import sbt._
import sbt.Keys._
import scala.Some

object Tool extends PluginDefaults {

  implicit val artifactPrefix = Some("org.openmole.plugin.tool")

  lazy val groovy = OsgiProject("groovy") dependsOn (provided(Misc.exception), provided(Core.model)) settings
    (libraryDependencies += Libraries.groovy % "provided")

  lazy val netLogoAPI = OsgiProject("netlogo") settings (autoScalaLibrary := false, crossPaths := false)

  lazy val netLogo4API = OsgiProject("netlogo4") dependsOn (netLogoAPI) settings (
    scalaVersion := "2.8.0",
    crossPaths := false,
    libraryDependencies ++= Seq(Libraries.netlogo4_noscala)
  )

  lazy val netLogo5API = OsgiProject("netlogo5") dependsOn (netLogoAPI) settings (
    scalaVersion := "2.9.2",
    crossPaths := false,
    libraryDependencies ++= Seq(Libraries.netlogo5_noscala)
  )

  lazy val csv = OsgiProject("csv") dependsOn (Misc.exception, Core.model) settings (
    libraryDependencies += Libraries.opencsv
  )

}