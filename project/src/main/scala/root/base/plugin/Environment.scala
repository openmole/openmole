package root.base.plugin

import root.base._
import root.libraries
import sbt._

package object environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.environment")

  lazy val all = Project("core-plugins-environment", dir) aggregate (desktopgrid, glite, gridscale, pbs, ssh)

  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn (core.model, misc.workspace, misc.tools,
    core.batch, provided(core.serializer), misc.sftpserver, libraries.gridscale)

  lazy val glite = OsgiProject("glite") dependsOn (core.model, misc.exception, gridscale, misc.updater, provided(core.batch),
    misc.workspace, provided(libraries.scalaLang), misc.fileService)

  lazy val gridscale = OsgiProject("gridscale") dependsOn (core.model, misc.workspace, misc.tools, core.implementation,
    provided(core.batch), libraries.gridscale, misc.exception)

  lazy val pbs = OsgiProject("pbs") dependsOn (misc.exception, gridscale, misc.workspace, provided(core.batch))

  lazy val ssh = OsgiProject("ssh") dependsOn (misc.exception, gridscale, misc.workspace, misc.eventDispatcher, provided(core.batch))
}