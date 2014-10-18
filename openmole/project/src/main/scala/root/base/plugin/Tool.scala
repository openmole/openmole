package root.base.plugin

import root.base._
import root.Libraries
import sbt._
import sbt.Keys._
import scala.Some

object Tool extends PluginDefaults {

  implicit val artifactPrefix = Some("org.openmole.plugin.tool")

  lazy val groovy = OsgiProject("groovy") dependsOn (provided(Misc.exception), provided(Core.implementation),
    provided(Libraries.groovy))

  lazy val netLogoAPI = OsgiProject("netlogo") settings (autoScalaLibrary := false, crossPaths := false)

  lazy val netLogo4API = OsgiProject("netlogo4") dependsOn (netLogoAPI, Libraries.netlogo4_noscala) settings (
    scalaVersion := "2.8.0",
    crossPaths := false
  )

  lazy val netLogo5API = OsgiProject("netlogo5") dependsOn (netLogoAPI, Libraries.netlogo5_noscala) settings (
    scalaVersion := "2.9.2",
    crossPaths := false
  )
}