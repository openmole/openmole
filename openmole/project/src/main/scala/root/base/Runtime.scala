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

  val runtime = OsgiProject("runtime", singleton = true) dependsOn (Core.implementation, Core.batch, Core.serializer, Misc.logging, scalaLang, scopt, Misc.eventDispatcher, Misc.exception) settings
    (includeOsgiProv,bundleType += "runtime")

  val daemon = OsgiProject("daemon", singleton = true, imports = Seq("*")) dependsOn (Core.model, Core.implementation, Core.batch, Misc.workspace,
    Misc.fileService, Misc.exception, Misc.tools, Misc.logging, plugin.Environment.desktopgrid, scalaLang, Apache.logging, jodaTime, scopt) settings (includeOsgiProv, bundleType += "daemon")

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set())

}
