package root.gui.plugin

import sbt._
import root.gui._

object Environment extends GUIPluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.environment")
  /*
  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn ( base.Misc.exception,
    base.plugin.Environment.desktopgrid)

  lazy val glite = OsgiProject("glite") dependsOn ( base.plugin.Environment.glite,
    base.Misc.exception, base.Core.batch)

  lazy val local = OsgiProject("local") dependsOn ( base.Misc.exception,
    base.Core.model, base.Misc.replication % "test")

  lazy val pbs = OsgiProject("pbs") dependsOn ( base.plugin.Environment.pbs,
    base.Misc.exception, base.Core.batch)

  lazy val sge = OsgiProject("sge") dependsOn ( base.plugin.Environment.sge,
    base.Misc.exception, base.Core.batch)

  lazy val oar = OsgiProject("oar") dependsOn ( base.plugin.Environment.oar,
    base.Misc.exception, base.Core.batch)

  lazy val condor = OsgiProject("condor") dependsOn ( base.plugin.Environment.condor,
    base.Misc.exception, base.Core.batch)

  lazy val slurm = OsgiProject("slurm") dependsOn ( base.plugin.Environment.slurm,
    base.Misc.exception, base.Core.batch)

  lazy val ssh = OsgiProject("ssh") dependsOn ( base.plugin.Environment.ssh,
    base.Core.batch)*/
}
