package root.gui.plugin

import root.base
import sbt._
import root.gui._
import root.Libraries._

object Sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.sampling")

  //FIXME: Depends on modifier for forward compatibility (update) remove in a few versions 
  lazy val combine = OsgiProject("combine") dependsOn (modifier, Core.implementation, base.plugin.Sampling.combine, base.Core.model, base.Misc.replication % "test")

  lazy val modifier = OsgiProject("modifier") dependsOn (Core.implementation, base.plugin.Sampling.modifier, base.Core.model, base.Misc.replication % "test")

  lazy val csv = OsgiProject("csv") dependsOn (opencsv, Core.implementation, base.plugin.Sampling.csv, base.Misc.exception, base.Misc.replication % "test")

  lazy val lhs = OsgiProject("lhs") dependsOn (base.plugin.Sampling.lhs, Core.implementation, base.plugin.Domain.range, Domain.range, base.Misc.replication % "test")

}
