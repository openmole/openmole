package root.base.plugin

import root.base._
import root.Libraries
import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object Environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.environment")

  lazy val oar = OsgiProject("oar", imports = Seq("*")) dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleOAR)

  lazy val desktopgrid = OsgiProject("desktopgrid", imports = Seq("*")) dependsOn (Core.workflow, Misc.workspace, Misc.tools,
    Core.batch, Core.serializer, plugin.Tool.sftpserver) //settings (bundleType += "daemon")

  lazy val egi = OsgiProject("egi", imports = Seq("!org.apache.http.*", "!fr.iscpif.gridscale.libraries.srmstub", "!fr.iscpif.gridscale.libraries.lbstub", "!fr.iscpif.gridscale.libraries.wmsstub", "*")) dependsOn (Core.workflow, Misc.exception, Misc.updater, Core.batch,
    Misc.workspace, Misc.fileService, gridscale) settings (
      libraryDependencies ++= Seq(Libraries.gridscaleGlite, Libraries.gridscaleDirac, Libraries.gridscaleHTTP, Libraries.scalaLang))

  lazy val gridscale = OsgiProject("gridscale", imports = Seq("*")) dependsOn (Core.workflow, Misc.workspace, Misc.tools, Core.workflow,
    Core.batch, Misc.exception)

  lazy val pbs = OsgiProject("pbs", imports = Seq("*")) dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscalePBS)

  lazy val sge = OsgiProject("sge", imports = Seq("*")) dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleSGE)

  lazy val condor = OsgiProject("condor", imports = Seq("*")) dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleCondor)

  lazy val slurm = OsgiProject("slurm", imports = Seq("*")) dependsOn (Misc.exception, Misc.workspace, Core.batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleSLURM)

  lazy val ssh = OsgiProject("ssh", imports = Seq("*")) dependsOn (Misc.exception, Misc.workspace, Misc.eventDispatcher, Core.batch, gridscale) settings
    (libraryDependencies += Libraries.gridscaleSSH)

}
