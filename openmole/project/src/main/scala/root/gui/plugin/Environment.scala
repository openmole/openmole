package root.gui.plugin

import root.base
import sbt._
import root.gui._

object Environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.environment")

  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn (Core.implementation, base.Misc.exception,
    base.plugin.Environment.desktopgrid)

  lazy val glite = OsgiProject("glite") dependsOn (Core.implementation, base.plugin.Environment.glite,
    base.Misc.exception, base.Core.batch)

  lazy val local = OsgiProject("local") dependsOn (Core.implementation, base.Misc.exception,
    base.Core.model)

  lazy val pbs = OsgiProject("pbs") dependsOn (Core.implementation, base.plugin.Environment.pbs,
    base.Misc.exception, base.Core.batch)

  lazy val ssh = OsgiProject("ssh") dependsOn (Core.implementation, base.plugin.Environment.ssh,
    base.Core.batch)
}
