package root.base.plugin

import root.base._
import root.Libraries
import sbt._

object Environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.environment")

  lazy val all = Project("core-plugins-environment", dir) aggregate (desktopgrid, glite, gridscale, pbs, ssh)

  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn (Core.model, Misc.workspace, Misc.tools,
    Core.batch, provided(Core.serializer), Misc.sftpserver, Libraries.gridscale)

  lazy val glite = OsgiProject("glite") dependsOn (Core.model, Misc.exception, gridscale, Misc.updater, provided(Core.batch),
    Misc.workspace, provided(Libraries.scalaLang), Misc.fileService)

  lazy val gridscale = OsgiProject("gridscale") dependsOn (Core.model, Misc.workspace, Misc.tools, Core.implementation,
    provided(Core.batch), Libraries.gridscale, Misc.exception)

  lazy val pbs = OsgiProject("pbs") dependsOn (Misc.exception, gridscale, Misc.workspace, provided(Core.batch))

  lazy val ssh = OsgiProject("ssh") dependsOn (Misc.exception, gridscale, Misc.workspace, Misc.eventDispatcher, provided(Core.batch))
}