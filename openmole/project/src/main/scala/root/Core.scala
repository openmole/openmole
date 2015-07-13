package root

import org.openmole.buildsystem.OMKeys._
import com.typesafe.sbt.osgi.OsgiKeys
import root.Libraries._
import root.ThirdParties._
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin._

object Core extends Defaults {
  override def dir = file("core")

  implicit val artifactPrefix = Some("org.openmole.core")

  lazy val workflow = OsgiProject("workflow", imports = Seq("*")) settings (
    includeOsgi,
    libraryDependencies ++= Seq(scalaLang, math, scalatest)
  ) dependsOn
    (event, exception, tools, updater, workspace, macros, pluginManager, serializer, output, console, replication % "test")

  lazy val serializer = OsgiProject("serializer", imports = Seq("*")) settings
    (includeOsgi,
      libraryDependencies += xstream) dependsOn
      (workspace, pluginManager, fileService, tools, openmoleTar)

  lazy val batch = OsgiProject("batch", imports = Seq("*")) dependsOn (
    workflow, workspace, tools, event, replication, updater, exception,
    serializer, fileService, pluginManager, openmoleTar) settings (libraryDependencies ++= Seq(gridscale, h2, guava, jasypt, slick, apacheConfig))

  lazy val dsl = OsgiProject("dsl", imports = Seq("*")) dependsOn (workflow, logging)

  val exception = OsgiProject("exception", imports = Seq("*"))

  val tools = OsgiProject("tools", dynamicImports = Seq("*"), imports = Seq("*")) settings
    (includeOsgi,
      libraryDependencies ++= Seq(xstream, exec, math, jodaTime, scalaLang, scalatest)) dependsOn
      (exception, openmoleTar, openmoleFile, openmoleLock, openmoleThread, openmoleHash, openmoleService, openmoleStream, openmoleCollection)

  val event = OsgiProject("event", imports = Seq("*")) dependsOn (tools) settings (
    libraryDependencies += scalatest
  )

  val replication = OsgiProject("replication", imports = Seq("*")) settings (bundleType += "dbserver",
    libraryDependencies ++= Seq(slick, xstream))

  val workspace = OsgiProject("workspace", imports = Seq("*")) settings
    (includeOsgi, libraryDependencies ++= Seq(jasypt, xstream, apacheConfig, math)) dependsOn
    (exception, event, tools, replication, openmoleCrypto)

  val fileDeleter = OsgiProject("filedeleter", imports = Seq("*")) settings (includeOsgi) dependsOn (tools)

  val macros = OsgiProject("macros", imports = Seq("*")) settings (libraryDependencies += scalaLang % "provided" /*, provided(scalaCompiler)*/ )

  val pluginManager = OsgiProject("pluginmanager",
    bundleActivator = Some("org.openmole.core.pluginmanager.internal.Activator"), imports = Seq("*")) settings
    (includeOsgi) dependsOn (exception, tools, workspace)

  val updater = OsgiProject("updater", imports = Seq("*")) dependsOn (exception, tools, workspace)

  val fileService = OsgiProject("fileservice", imports = Seq("*")) settings (includeOsgi) dependsOn (tools, updater, workspace, fileDeleter, openmoleTar % "provided")

  val logging = OsgiProject(
    "logging",
    bundleActivator = Some("org.openmole.core.logging.internal.Activator"), imports = Seq("*")) settings (libraryDependencies ++= Seq(log4j, logback, slf4j, equinoxCommon)) dependsOn
    (tools, workspace)

  val output = OsgiProject("output", imports = Seq("*"))

  val console = OsgiProject("console", bundleActivator = Some("org.openmole.core.console.Activator"), dynamicImports = Seq("*"), imports = Seq("*")) dependsOn
    (pluginManager) settings (includeOsgi, OsgiKeys.importPackage := Seq("*"), libraryDependencies += scalaLang)

  val buildinfo = OsgiProject("buildinfo", imports = Seq("*")) settings (
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
            }),
        buildInfoPackage := s"${artifactPrefix.get}.buildinfo"
      ): _*
  )

  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set("core", "runtime"), OSGi.openMOLEScope := Some("provided"))
}
