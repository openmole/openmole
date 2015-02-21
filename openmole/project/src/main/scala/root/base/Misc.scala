package root.base

import root.BaseDefaults
import sbt._
import Keys._

import org.openmole.buildsystem.OMKeys._
import com.typesafe.sbt.osgi.OsgiKeys

object Misc extends BaseDefaults {

  import root.Libraries._
  import root.libraries.Apache
  import root.ThirdParties._

  override val dir = file("core/misc")

  val exception = OsgiProject("org.openmole.misc.exception", imports = Seq("*"))

  val osgi = OsgiProject("org.openmole.misc.osgi", buddyPolicy = Some("global"), imports = Seq("*"),
    bundleActivator = Some("org.openmole.misc.osgi.Activator")) dependsOn (exception) settings
    (includeOsgi, libraryDependencies += scalaLang)

  val tools = OsgiProject("org.openmole.misc.tools", buddyPolicy = Some("global"), imports = Seq("*")) settings
    (includeOsgi,
      libraryDependencies ++= Seq(xstream, groovy, Apache.exec, Apache.math, jodaTime, scalaLang, scalatest)) dependsOn
      (exception, osgi, iceTar)

  val eventDispatcher = OsgiProject("org.openmole.misc.eventdispatcher", imports = Seq("*")) dependsOn (tools) settings (
    libraryDependencies += scalatest
  )

  val replication = OsgiProject("org.openmole.misc.replication", imports = Seq("*")) settings (bundleType += "dbserver",
    libraryDependencies ++= Seq(slick, xstream))

  val workspace = OsgiProject("org.openmole.misc.workspace", imports = Seq("*")) settings
    (includeOsgi, libraryDependencies ++= Seq(jasypt, xstream, Apache.config, Apache.math)) dependsOn
    (osgi, exception, eventDispatcher, tools, replication)

  val fileDeleter = OsgiProject("org.openmole.misc.filedeleter", imports = Seq("*")) settings (includeOsgi) dependsOn (tools)

  val fileCache = OsgiProject("org.openmole.misc.filecache", imports = Seq("*")) settings (includeOsgi) dependsOn (fileDeleter)

  val macros = OsgiProject("org.openmole.misc.macros", imports = Seq("*")) settings (libraryDependencies += scalaLang % "provided" /*, provided(scalaCompiler)*/ )

  val pluginManager = OsgiProject("org.openmole.misc.pluginmanager",
    bundleActivator = Some("org.openmole.misc.pluginmanager.internal.Activator"), imports = Seq("*")) settings
    (includeOsgi) dependsOn (exception, tools, osgi)

  val updater = OsgiProject("org.openmole.misc.updater", imports = Seq("*")) dependsOn (exception, tools, workspace)

  val fileService = OsgiProject("org.openmole.misc.fileservice", imports = Seq("*")) settings (includeOsgi) dependsOn (tools, fileCache, updater, workspace, iceTar % "provided")

  val logging = OsgiProject(
    "org.openmole.misc.logging",
    bundleActivator = Some("org.openmole.misc.logging.internal.Activator"), imports = Seq("*")) settings (libraryDependencies ++= Seq(Apache.log4j, logback, slf4j, equinoxCommon)) dependsOn
    (tools, workspace)

  val console = OsgiProject("org.openmole.misc.console", bundleActivator = Some("org.openmole.misc.console.Activator"), buddyPolicy = Some("global"), imports = Seq("*")) dependsOn
    (osgi, pluginManager) settings (includeOsgi, OsgiKeys.importPackage := Seq("*"), libraryDependencies += scalaLang)

}
