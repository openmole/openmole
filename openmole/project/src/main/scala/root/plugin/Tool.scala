package plugin

import root.base.Misc._
import root.Libraries
import root.libraries.Apache
import sbt._
import sbt.Keys._
import com.typesafe.sbt.osgi.OsgiKeys._
import root._
import root.base._

object Tool extends PluginDefaults {

  implicit val artifactPrefix = Some("org.openmole.plugin.tool")

  lazy val groovy = OsgiProject("groovy", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow) settings (
    libraryDependencies += Libraries.groovy
  )

  lazy val netLogoAPI = OsgiProject("netlogo", imports = Seq("*")) settings (autoScalaLibrary := false, crossPaths := false)

  lazy val netLogo4API = OsgiProject("netlogo4", imports = Seq("*")) dependsOn (netLogoAPI) settings (
    scalaVersion := "2.8.0",
    crossPaths := false,
    libraryDependencies += Libraries.netlogo4_noscala
  )

  lazy val netLogo5API = OsgiProject("netlogo5", imports = Seq("*")) dependsOn (netLogoAPI) settings (
    scalaVersion := "2.9.2",
    crossPaths := false,
    libraryDependencies += Libraries.netlogo5_noscala
  )

  lazy val csv = OsgiProject("csv", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow) settings (
    libraryDependencies += Libraries.opencsv
  )

  val sftpserver = OsgiProject("sftpserver", imports = Seq("*")) dependsOn (Misc.tools) settings (libraryDependencies += Apache.sshd)

  override def osgiSettings = super.osgiSettings ++ Seq(bundleActivator := None)

}