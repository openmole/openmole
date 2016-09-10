package plugin

import java.nio.file.{ CopyOption, Files, StandardCopyOption }

import root.Libraries
import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys
import org.openmole.buildsystem.OMKeys.{ Tar, _ }
import root._

object Environment extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.environment")

  lazy val batch = {
    lazy val copyRuntime = TaskKey[Unit]("copyRuntime")

    OsgiProject("batch", imports = Seq("*")) dependsOn (
      Core.workflow, Core.workspace, Core.tools, Core.event, Core.replication, Core.updater, Core.exception,
      Core.serializer, Core.fileService, Core.pluginManager, ThirdParties.openmoleTar, Core.communication, Bin.openmoleRuntime
    ) settings (
        copyRuntime := (Tar.tar in Bin.openmoleRuntime, crossTarget).map {
          (tar, target) ⇒
            val dest = new File(target, "classes/org/openmole/plugin/environment/batch/")
            dest.mkdirs()
            Files.copy(tar.toPath, new File(dest, "runtime.tar.gz").toPath, StandardCopyOption.REPLACE_EXISTING)
        }.value,
        (copyResources in Compile) := ((copyResources in Compile) dependsOn copyRuntime).value,
        cleanFiles <++= cleanFiles in Bin.openmoleRuntime,
        libraryDependencies ++= Seq(
          Libraries.gridscale,
          Libraries.h2,
          Libraries.guava,
          Libraries.jasypt,
          Libraries.slick
        )
      )
  }

  lazy val oar = OsgiProject("oar", imports = Seq("*")) dependsOn (Core.dsl, batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleOAR)

  lazy val desktopgrid = OsgiProject("desktopgrid", imports = Seq("*")) dependsOn (
    Core.dsl,
    batch, Tool.sftpserver, gridscale
  )

  lazy val egi = OsgiProject("egi", imports = Seq("!org.apache.http.*", "!fr.iscpif.gridscale.libraries.srmstub", "!fr.iscpif.gridscale.libraries.lbstub", "!fr.iscpif.gridscale.libraries.wmsstub", "!com.google.common.cache", "*")) dependsOn (Core.dsl, Core.updater, batch,
    Core.workspace, Core.fileService, gridscale) settings (
      libraryDependencies ++= Seq(Libraries.gridscaleGlite, Libraries.gridscaleHTTP, Libraries.scalaLang)
    )

  lazy val gridscale = OsgiProject("gridscale", imports = Seq("*")) dependsOn (Core.dsl, Core.tools,
    batch, Core.exception)

  lazy val pbs = OsgiProject("pbs", imports = Seq("*")) dependsOn (Core.dsl, batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscalePBS)

  lazy val sge = OsgiProject("sge", imports = Seq("*")) dependsOn (Core.dsl, batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleSGE)

  lazy val condor = OsgiProject("condor", imports = Seq("*")) dependsOn (Core.dsl, batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleCondor)

  lazy val slurm = OsgiProject("slurm", imports = Seq("*")) dependsOn (Core.dsl, batch, gridscale, ssh) settings
    (libraryDependencies += Libraries.gridscaleSLURM)

  lazy val ssh = OsgiProject("ssh", imports = Seq("*")) dependsOn (Core.dsl, Core.event, batch, gridscale) settings
    (libraryDependencies += Libraries.gridscaleSSH)

}
