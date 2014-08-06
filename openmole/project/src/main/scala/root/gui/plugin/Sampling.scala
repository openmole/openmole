package root.gui.plugin

import root.base
import sbt._
import root.gui._
import root.Libraries._

object Sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.sampling")

  //FIXME: Depends on modifier for forward compatibility (update) remove in a few versions 
  lazy val combine = OsgiProject("combine") dependsOn (modifier, Ext.dataui, base.plugin.Sampling.combine, base.Core.model, base.Misc.replication % "test")

  lazy val modifier = OsgiProject("modifier") dependsOn (Ext.dataui, base.plugin.Sampling.combine, base.Core.model, base.Misc.replication % "test")

  lazy val csv = OsgiProject("csv") dependsOn (opencsv, Ext.dataui, base.plugin.Sampling.csv, base.Misc.exception, base.Misc.replication % "test")

  lazy val lhs = OsgiProject("lhs") dependsOn (base.plugin.Sampling.lhs, Ext.dataui, base.plugin.Domain.range, Domain.range, base.Misc.replication % "test")

}
