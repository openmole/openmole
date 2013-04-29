package root.gui.plugin

import root.base
import sbt._
import root.gui._
import root.libraries._

package object sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.sampling")

  lazy val all = Project("gui-plugin-sampling", dir) aggregate (combine, csv, lhs)

  lazy val combine = OsgiProject("combine") dependsOn (core.implementation, base.plugin.sampling.combine, base.core.model)

  lazy val csv = OsgiProject("csv") dependsOn (opencsv, core.implementation, base.plugin.sampling.csv, base.misc.exception)

  lazy val lhs = OsgiProject("lhs") dependsOn (base.plugin.sampling.lhs, core.implementation, base.plugin.domain.bounded,
    domain.range)

}