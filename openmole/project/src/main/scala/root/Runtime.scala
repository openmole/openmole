package root

import org.openmole.buildsystem.OMKeys._
import root.Libraries._
import sbt.Keys._
import sbt._

object Runtime extends Defaults(runtime.REST) {
  override def dir = file("runtime")

  val dbserver = OsgiProject("org.openmole.runtime.dbserver", imports = Seq("*")) dependsOn (Core.replication) settings (bundleType += "dbserver",
    libraryDependencies ++= Seq(h2, slf4j, xstream))

  val runtime = OsgiProject("org.openmole.runtime.runtime", singleton = true, imports = Seq("*")) dependsOn (Core.workflow, Core.batch, Core.serializer, Core.logging, Core.event, Core.exception) settings
    (bundleType += "runtime", libraryDependencies ++= Seq(scalaLang, scopt, equinoxCommon, equinoxApp, equinoxOSGi))

  val daemon = OsgiProject("org.openmole.runtime.daemon", singleton = true, imports = Seq("*")) dependsOn (Core.workflow, Core.workflow, Core.batch, Core.workspace,
    Core.fileService, Core.exception, Core.tools, Core.logging, plugin.Environment.desktopgrid) settings
    (bundleType += "daemon",
      libraryDependencies ++= Seq(scalaLang, logging, jodaTime, scopt, equinoxCommon, equinoxApp, gridscaleSSH, equinoxOSGi))

  lazy val console = OsgiProject("org.openmole.runtime.console", imports = Seq("*")) settings (
    libraryDependencies += upickle
  ) dependsOn (
      Core.workflow,
      Core.console,
      Core.project,
      Core.dsl,
      Core.batch,
      Core.buildinfo
    )

  lazy val launcher = OsgiProject("org.openmole.runtime.launcher", imports = Seq("*")) settings (
    libraryDependencies += equinoxOSGi
  )

  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set())

}
