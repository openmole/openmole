package root.base.plugin

import root.base._
import root.Libraries
import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object Environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.environment")

  lazy val oar = OsgiProject("oar") dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleOAR)

  lazy val desktopgrid = OsgiProject("desktopgrid") dependsOn (Core.model, Misc.workspace, Misc.tools,
    Core.batch, Core.serializer, Misc.sftpserver) settings (bundleType += "daemon")

  lazy val glite = OsgiProject("glite") dependsOn (Core.model, Misc.exception, Misc.updater, Core.batch,
    Misc.workspace, Misc.fileService, gridscale) settings (
      libraryDependencies ++= Seq(Libraries.gridscaleGlite, Libraries.gridscaleDirac, Libraries.gridscaleHTTP, Libraries.scalaLang % "provided"))

  lazy val gridscale = OsgiProject("gridscale") dependsOn (Core.model, Misc.workspace, Misc.tools, Core.implementation,
    provided(Core.batch), Misc.exception)

  lazy val pbs = OsgiProject("pbs") dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscalePBS)

  lazy val sge = OsgiProject("sge") dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleSGE)

  lazy val condor = OsgiProject("condor") dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleCondor)

  lazy val slurm = OsgiProject("slurm") dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleSLURM)

  lazy val ssh = OsgiProject("ssh") dependsOn (Misc.exception, Misc.workspace, Misc.eventDispatcher, Core.batch, gridscale) settings
    (libraryDependencies ++= Libraries.gridscaleSSH)

}
