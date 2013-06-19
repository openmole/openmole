package root.base.plugin

import root.base._
import root.Libraries
import sbt._
import Keys._

object Environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.environment")

  lazy val all = Project("core-plugins-environment", dir) aggregate (desktopgrid, glite, gridscale, pbs, ssh)

  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn (Core.model, Misc.workspace, Misc.tools,
    Core.batch, provided(Core.serializer), Misc.sftpserver)

  lazy val glite = OsgiProject("glite") dependsOn (Core.model, Misc.exception, Misc.updater, provided(Core.batch),
    Misc.workspace, provided(Libraries.scalaLang), Misc.fileService, gridscale) settings
    (libraryDependencies += "fr.iscpif.gridscale" % "glite-bundle" % Libraries.gridscaleVersion,
      libraryDependencies += "fr.iscpif.gridscale" % "dirac-bundle" % Libraries.gridscaleVersion,
      libraryDependencies += "fr.iscpif.gridscale" % "http-bundle" % Libraries.gridscaleVersion)

  lazy val gridscale = OsgiProject("gridscale") dependsOn (Core.model, Misc.workspace, Misc.tools, Core.implementation,
    provided(Core.batch), Misc.exception)

  lazy val pbs = OsgiProject("pbs") dependsOn (Misc.exception, Misc.workspace, provided(Core.batch), gridscale, ssh) settings
    (libraryDependencies += "fr.iscpif.gridscale" % "pbs-bundle" % Libraries.gridscaleVersion)

  lazy val ssh = OsgiProject("ssh") dependsOn (Misc.exception, Misc.workspace, Misc.eventDispatcher, provided(Core.batch), gridscale) settings
    (libraryDependencies += "fr.iscpif.gridscale" % "ssh-bundle" % Libraries.gridscaleVersion)

}
