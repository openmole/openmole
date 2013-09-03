package root.base.plugin

import root.base._
import root.Libraries
import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object Environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.environment")

  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn (Core.model, Misc.workspace, Misc.tools,
    Core.batch, provided(Core.serializer), Misc.sftpserver) settings (bundleType += "daemon")

  lazy val glite = OsgiProject("glite") dependsOn (Core.model, Misc.exception, Misc.updater, provided(Core.batch),
    Misc.workspace, provided(Libraries.scalaLang), Misc.fileService, gridscale) settings
    (Libraries.includeGridscaleGlite, Libraries.includeGridscaleDirac, Libraries.includeGridscaleHTTP)

  lazy val gridscale = OsgiProject("gridscale") dependsOn (Core.model, Misc.workspace, Misc.tools, Core.implementation,
    provided(Core.batch), Misc.exception)

  lazy val pbs = OsgiProject("pbs") dependsOn (Misc.exception, Misc.workspace, provided(Core.batch), gridscale, ssh) settings
    (Libraries.includeGridscalePBS)

  lazy val ssh = OsgiProject("ssh") dependsOn (Misc.exception, Misc.workspace, Misc.eventDispatcher, provided(Core.batch), gridscale) settings
    (Libraries.includeGridscaleSSH)

}
