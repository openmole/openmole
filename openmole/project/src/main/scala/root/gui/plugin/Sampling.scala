package root.gui.plugin

import root.base
import sbt._
import root.gui._
import root.Libraries._

object Sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.sampling")

  lazy val combine = OsgiProject("combine") dependsOn (Core.implementation, base.plugin.Sampling.combine, base.Core.model, base.Misc.replication % "test")

  lazy val csv = OsgiProject("csv") dependsOn (opencsv, Core.implementation, base.plugin.Sampling.csv, base.Misc.exception, base.Misc.replication % "test")

  lazy val lhs = OsgiProject("lhs") dependsOn (base.plugin.Sampling.lhs, Core.implementation, base.plugin.Domain.bounded,
    Domain.range, base.Misc.replication % "test")

}