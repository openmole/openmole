package root.gui.plugin

import root.base
import sbt._
import root.gui._

object Environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.environment")

  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn (Ext.dataui, base.Misc.exception,
    base.plugin.Environment.desktopgrid)

  lazy val glite = OsgiProject("glite") dependsOn (Ext.dataui, base.plugin.Environment.glite,
    base.Misc.exception, base.Core.batch)

  lazy val local = OsgiProject("local") dependsOn (Ext.dataui, base.Misc.exception,
    base.Core.model, base.Misc.replication % "test")

  lazy val pbs = OsgiProject("pbs") dependsOn (Ext.dataui, base.plugin.Environment.pbs,
    base.Misc.exception, base.Core.batch)

  lazy val sge = OsgiProject("sge") dependsOn (Ext.dataui, base.plugin.Environment.sge,
    base.Misc.exception, base.Core.batch)

  lazy val oar = OsgiProject("oar") dependsOn (Ext.dataui, base.plugin.Environment.oar,
    base.Misc.exception, base.Core.batch)

  lazy val condor = OsgiProject("condor") dependsOn (Ext.dataui, base.plugin.Environment.condor,
    base.Misc.exception, base.Core.batch)

  lazy val slurm = OsgiProject("slurm") dependsOn (Ext.dataui, base.plugin.Environment.slurm,
    base.Misc.exception, base.Core.batch)

  lazy val ssh = OsgiProject("ssh") dependsOn (Ext.dataui, base.plugin.Environment.ssh,
    base.Core.batch)
}
