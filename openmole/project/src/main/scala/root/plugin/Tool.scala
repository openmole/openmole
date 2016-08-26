package plugin

import root.Libraries
import sbt._
import sbt.Keys._
import com.typesafe.sbt.osgi.OsgiKeys._
import root._

object Tool extends PluginDefaults {

  implicit val artifactPrefix = Some("org.openmole.plugin.tool")

  lazy val netLogoAPI = OsgiProject("netlogo", imports = Seq("*")) settings (autoScalaLibrary := false, crossPaths := false)

  lazy val netLogo5API = OsgiProject("netlogo5", imports = Seq("*")) dependsOn (netLogoAPI) settings (
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies += Libraries.netlogo5 intransitive (),
    libraryDependencies -= Libraries.scalatest
  )

  lazy val csv = OsgiProject("csv", imports = Seq("*")) dependsOn (Core.exception, Core.dsl) settings (
    libraryDependencies += Libraries.opencsv,
    defaultActivator
  )

  lazy val pattern = OsgiProject("pattern", imports = Seq("*")) dependsOn (Core.exception, Core.dsl) settings (defaultActivator)

  val sftpserver = OsgiProject("sftpserver", imports = Seq("*")) dependsOn (Core.tools) settings (libraryDependencies += Libraries.sshd)

  override def osgiSettings = super.osgiSettings ++ Seq(bundleActivator := None)

}
