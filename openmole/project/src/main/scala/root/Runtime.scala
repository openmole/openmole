package root

import org.openmole.buildsystem.OMKeys._
import root.Libraries._
import sbt.Keys._
import sbt._

object Runtime extends Defaults {
  implicit val artifactPrefix = Some("org.openmole.runtime")

  override def dir = file("runtime")

  val dbserver = OsgiProject("dbserver", imports = Seq("*")) dependsOn (Core.replication) settings (bundleType += "dbserver",
    libraryDependencies ++= Seq(h2, slf4j, xstream))

  val runtime = OsgiProject("runtime", singleton = true, imports = Seq("*")) dependsOn (Core.workflow, Core.batch, Core.serializer, Core.logging, Core.eventDispatcher, Core.exception) settings
    (includeOsgi, bundleType += "runtime", libraryDependencies ++= Seq(scalaLang, scopt, equinoxCommon, equinoxApp))

  val daemon = OsgiProject("daemon", singleton = true, imports = Seq("*")) dependsOn (Core.workflow, Core.workflow, Core.batch, Core.workspace,
    Core.fileService, Core.exception, Core.tools, Core.logging, plugin.Environment.desktopgrid) settings
    (includeOsgi, bundleType += "daemon",
      libraryDependencies ++= Seq(scalaLang, logging, jodaTime, scopt, equinoxCommon, equinoxApp, gridscaleSSH)
    )

  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set())

}
