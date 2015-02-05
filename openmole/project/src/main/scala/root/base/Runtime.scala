package root.base

import org.openmole.buildsystem.OMKeys._

import root.BaseDefaults
import sbt._
import Keys._
import root.Libraries._
import root.libraries.Apache
import root.ThirdParties._

object Runtime extends BaseDefaults {
  implicit val artifactPrefix = Some("org.openmole.runtime")

  override def dir = super.dir / "runtime"

  val dbserver = OsgiProject("dbserver") dependsOn (Misc.replication) settings (bundleType += "dbserver",
    libraryDependencies ++= Seq(h2, slf4j, xstream))

  val runtime = OsgiProject("runtime", singleton = true) dependsOn (Core.workflow, Core.batch, Core.serializer, Misc.logging, Misc.eventDispatcher, Misc.exception) settings
    (includeOsgi, bundleType += "runtime", libraryDependencies ++= Seq(scalaLang, scopt, equinoxCommon, equinoxApp))

  val daemon = OsgiProject("daemon", singleton = true, imports = Seq("*")) dependsOn (Core.workflow, Core.workflow, Core.batch, Misc.workspace,
    Misc.fileService, Misc.exception, Misc.tools, Misc.logging, plugin.Environment.desktopgrid) settings
    (includeOsgi, bundleType += "daemon",
      libraryDependencies ++= Seq(scalaLang, Apache.logging, jodaTime, scopt, equinoxCommon, equinoxApp, gridscaleSSH)
    )

  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set())

}
