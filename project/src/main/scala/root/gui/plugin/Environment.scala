package root.gui.plugin

import root.base
import sbt._
import root.gui._

package object environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.environment")

  lazy val all = Project("gui-plugin-environment", dir) aggregate (desktopgrid, glite, local, pbs, ssh)

  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn (core.implementation, base.misc.exception,
    base.plugin.environment.desktopgrid)

  lazy val glite = OsgiProject("glite") dependsOn (core.implementation, base.plugin.environment.glite,
    base.misc.exception, base.core.batch)

  lazy val local = OsgiProject("local") dependsOn (core.implementation, base.misc.exception,
    base.core.model)

  lazy val pbs = OsgiProject("pbs") dependsOn (core.implementation, base.plugin.environment.pbs,
    base.misc.exception, base.core.batch)

  lazy val ssh = OsgiProject("ssh") dependsOn (core.implementation, base.plugin.environment.ssh,
    base.core.batch)
}