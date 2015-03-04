package root.gui.plugin

import sbt._
import Keys._
import root.gui._
import root.Libraries._

object Sampling extends GUIPluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.sampling")
  /*
  //FIXME: Depends on modifier for forward compatibility (update) remove in a few versions 
  lazy val combine = OsgiProject("combine") dependsOn (modifier, base.plugin.Sampling.combine, base.Core.model, base.Misc.replication % "test")

  lazy val modifier = OsgiProject("modifier") dependsOn (base.plugin.Sampling.combine, base.Core.model, base.Misc.replication % "test")

  lazy val csv = OsgiProject("csv") dependsOn (base.plugin.Sampling.csv, base.Misc.exception, base.Misc.replication % "test") settings (libraryDependencies += opencsv)

  lazy val lhs = OsgiProject("lhs") dependsOn (base.plugin.Sampling.lhs, base.plugin.Domain.range, Domain.range, base.Misc.replication % "test")
*/
}
