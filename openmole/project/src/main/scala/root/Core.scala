package root

import org.openmole.buildsystem.OMKeys._
import com.typesafe.sbt.osgi.OsgiKeys
import root.Libraries._
import root.ThirdParties._
import sbt.Keys._
import sbt._

object Core extends Defaults {
  override val org = "org.openmole.core"
  override def dir = file("core")

  implicit val artifactPrefix = Some("org.openmole.core")

  lazy val workflow = OsgiProject("workflow", imports = Seq("*")) settings (
    includeOsgi,
    libraryDependencies ++= Seq(scalaLang, groovy, math, scalatest)
  ) dependsOn
    (eventDispatcher, exception, tools, updater, workspace, macros, pluginManager, serializer, replication % "test")

  lazy val serializer = OsgiProject("serializer", imports = Seq("*")) settings
    (includeOsgi,
      libraryDependencies += xstream) dependsOn
      (workspace, pluginManager, fileService, tools, iceTar)

  lazy val batch = OsgiProject("batch", imports = Seq("*")) dependsOn (
    workflow, workspace, tools, eventDispatcher, replication, updater, exception,
    serializer, fileService, pluginManager, iceTar) settings (libraryDependencies ++= Seq(gridscale, h2, guava, jasypt, slick, apacheConfig))

  lazy val dsl = OsgiProject("dsl", imports = Seq("*")) dependsOn (workflow, logging)

  val exception = OsgiProject("exception", imports = Seq("*"))

  val tools = OsgiProject("tools", dynamicImports = Seq("*"), imports = Seq("*")) settings
    (includeOsgi,
      libraryDependencies ++= Seq(xstream, groovy, exec, math, jodaTime, scalaLang, scalatest)) dependsOn
      (exception, iceTar)

  val eventDispatcher = OsgiProject("eventdispatcher", imports = Seq("*")) dependsOn (tools) settings (
    libraryDependencies += scalatest
  )

  val replication = OsgiProject("replication", imports = Seq("*")) settings (bundleType += "dbserver",
    libraryDependencies ++= Seq(slick, xstream))

  val workspace = OsgiProject("workspace", imports = Seq("*")) settings
    (includeOsgi, libraryDependencies ++= Seq(jasypt, xstream, apacheConfig, math)) dependsOn
    (exception, eventDispatcher, tools, replication)

  val fileDeleter = OsgiProject("filedeleter", imports = Seq("*")) settings (includeOsgi) dependsOn (tools)

  val fileCache = OsgiProject("filecache", imports = Seq("*")) settings (includeOsgi) dependsOn (fileDeleter)

  val macros = OsgiProject("macros", imports = Seq("*")) settings (libraryDependencies += scalaLang % "provided" /*, provided(scalaCompiler)*/ )

  val pluginManager = OsgiProject("pluginmanager",
    bundleActivator = Some("org.openmole.core.pluginmanager.internal.Activator"), imports = Seq("*")) settings
    (includeOsgi) dependsOn (exception, tools, workspace)

  val updater = OsgiProject("updater", imports = Seq("*")) dependsOn (exception, tools, workspace)

  val fileService = OsgiProject("fileservice", imports = Seq("*")) settings (includeOsgi) dependsOn (tools, fileCache, updater, workspace, iceTar % "provided")

  val logging = OsgiProject(
    "logging",
    bundleActivator = Some("org.openmole.core.logging.internal.Activator"), imports = Seq("*")) settings (libraryDependencies ++= Seq(log4j, logback, slf4j, equinoxCommon)) dependsOn
    (tools, workspace)

  val console = OsgiProject("console", bundleActivator = Some("org.openmole.core.console.Activator"), dynamicImports = Seq("*"), imports = Seq("*")) dependsOn
    (pluginManager) settings (includeOsgi, OsgiKeys.importPackage := Seq("*"), libraryDependencies += scalaLang)

  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set("core"), OSGi.openMOLEScope := Some("provided"))
}
