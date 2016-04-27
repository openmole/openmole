package root

import org.openmole.buildsystem.OMKeys._
import com.typesafe.sbt.osgi.OsgiKeys
import org.scalajs.sbtplugin.ScalaJSPlugin
import root.Libraries._
import root.ThirdParties._
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin._

object Core extends Defaults {
  override def dir = file("core")

  implicit val artifactPrefix = Some("org.openmole.core")

  lazy val workflow = OsgiProject("workflow", imports = Seq("*")) settings (
    libraryDependencies ++= Seq(scalaLang, math, scalatest, scalaz, equinoxOSGi)
  ) dependsOn
    (event, exception, tools, updater, workspace, macros, pluginManager, serializer, output, console, replication % "test")

  lazy val serializer = OsgiProject("serializer", global = true, imports = Seq("*")) settings
    (libraryDependencies += xstream, libraryDependencies += equinoxOSGi) dependsOn (workspace, pluginManager, fileService, tools, openmoleTar, console)

  lazy val batch = OsgiProject("batch", imports = Seq("*")) dependsOn (
    workflow, workspace, tools, event, replication, updater, exception,
    serializer, fileService, pluginManager, openmoleTar
  ) settings (libraryDependencies ++= Seq(gridscale, h2, guava, jasypt, slick))

  lazy val dsl = OsgiProject("dsl", imports = Seq("*")) dependsOn (workflow, logging)

  val exception = OsgiProject("exception", imports = Seq("*"))

  val tools = OsgiProject("tools", global = true, imports = Seq("*")) settings
    (libraryDependencies ++= Seq(xstream, exec, math, jodaTime, scalaLang, scalatest, equinoxOSGi)) dependsOn
    (exception, openmoleTar, openmoleFile, openmoleLock, openmoleThread, openmoleHash, openmoleLogger, openmoleStream, openmoleCollection, openmoleStatistics, openmoleTypes)

  val event = OsgiProject("event", imports = Seq("*")) dependsOn (tools) settings (
    libraryDependencies += scalatest
  )

  val replication = OsgiProject("replication", imports = Seq("*")) settings (bundleType += "dbserver",
    libraryDependencies ++= Seq(slick, xstream))

  val workspace = OsgiProject("workspace", imports = Seq("*")) settings
    (libraryDependencies ++= Seq(jasypt, xstream, math)) dependsOn
    (exception, event, tools, replication, openmoleCrypto)

  val macros = OsgiProject("macros", imports = Seq("*")) settings (libraryDependencies += scalaLang % "provided" /*, provided(scalaCompiler)*/ )

  val pluginManager = OsgiProject(
    "pluginmanager",
    bundleActivator = Some("org.openmole.core.pluginmanager.internal.Activator"), imports = Seq("*")
  ) dependsOn (exception, tools, workspace)

  val updater = OsgiProject("updater", imports = Seq("*")) dependsOn (exception, tools, workspace)

  val fileService = OsgiProject("fileservice", imports = Seq("*")) dependsOn (tools, updater, workspace, openmoleTar % "provided")

  val logging = OsgiProject(
    "logging",
    bundleActivator = Some("org.openmole.core.logging.internal.Activator"), imports = Seq("*")
  ) settings (libraryDependencies ++= Seq(log4j, logback, slf4j)) dependsOn (tools, workspace)

  val output = OsgiProject("output", imports = Seq("*"))

  val console = OsgiProject("console", bundleActivator = Some("org.openmole.core.console.Activator"), global = true, imports = Seq("*")) dependsOn
    (pluginManager) settings (
      OsgiKeys.importPackage := Seq("*"),
      libraryDependencies += scalaLang,
      libraryDependencies += monocle,
      Defaults.macroParadise
    )

  val project = OsgiProject("project", imports = Seq("*")) dependsOn (console, dsl) settings (OsgiKeys.importPackage := Seq("*"))

  val buildinfo = OsgiProject("buildinfo", imports = Seq("*")) enablePlugins (ScalaJSPlugin) settings (
    buildInfoSettings ++
      Seq(
        sourceGenerators in Compile <+= buildInfo,
        buildInfoKeys :=
          Seq[BuildInfoKey](
            name,
            version,
            scalaVersion,
            sbtVersion,
            BuildInfoKey.action("buildTime") {
              System.currentTimeMillis
            }
          ),
        buildInfoPackage := s"${artifactPrefix.get}.buildinfo"
      ): _*
  )

  override def settings =
    super.settings ++
      Seq(
        libraryDependencies += Libraries.scalatest,
        libraryDependencies += Libraries.equinoxOSGi
      )

  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set("core", "runtime"), OSGi.openMOLEScope := Some("provided"))
}
