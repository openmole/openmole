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

  val exception = OsgiProject("org.openmole.misc.exception")

  val osgi = OsgiProject("org.openmole.misc.osgi", buddyPolicy = Some("global"), imports = Seq("*"),
    bundleActivator = Some("org.openmole.misc.osgi.Activator")) dependsOn (provided(exception)) settings
    (includeOsgiProv, libraryDependencies += scalaLang)

  val tools = OsgiProject("org.openmole.misc.tools", buddyPolicy = Some("global")) settings
    (includeOsgiProv, libraryDependencies ++= Seq(xstream % "provided", groovy, Apache.exec, Apache.pool % "provided",
      Apache.math % "provided", jodaTime % "provided", scalaLang % "provided")) dependsOn
      (provided(exception), osgi % "provided", iceTar)

  val eventDispatcher = OsgiProject("org.openmole.misc.eventdispatcher") dependsOn (provided(tools))

  val replication = OsgiProject("org.openmole.misc.replication") settings (bundleType += "dbserver",
    libraryDependencies ++= Seq(slick, xstream))

  val workspace = OsgiProject("org.openmole.misc.workspace") settings
    (includeOsgiProv, libraryDependencies ++= Seq(jasypt, xstream, Apache.config, Apache.math)) dependsOn
    (osgi, exception, eventDispatcher, tools, replication)

  val hashService = OsgiProject("org.openmole.misc.hashservice") settings
    (includeOsgiProv, libraryDependencies ++= Seq(gnuCrypto, Apache.pool)) dependsOn
    (exception, tools)

  val fileDeleter = OsgiProject("org.openmole.misc.filedeleter") settings (includeOsgiProv) dependsOn (provided(tools))

  val fileCache = OsgiProject("org.openmole.misc.filecache") settings (includeOsgiProv) dependsOn (fileDeleter)

  val macros = OsgiProject("org.openmole.misc.macros") settings (libraryDependencies += scalaLang % "provided" /*, provided(scalaCompiler)*/ )

  val pluginManager = OsgiProject("org.openmole.misc.pluginmanager",
    bundleActivator = Some("org.openmole.misc.pluginmanager.internal.Activator")) settings
    (includeOsgiProv) dependsOn (provided(exception), provided(tools), osgi)

  val updater = OsgiProject("org.openmole.misc.updater") dependsOn (provided(exception), provided(tools),
    provided(workspace))

  val fileService = OsgiProject("org.openmole.misc.fileservice") settings
    (includeOsgiProv) dependsOn
    (provided(tools), provided(hashService), fileCache, provided(updater), provided(workspace), provided(iceTar))

  val logging = OsgiProject(
    "org.openmole.misc.logging",
    bundleActivator = Some("org.openmole.misc.logging.internal.Activator")) dependsOn (provided(tools), provided(workspace)) settings
    (libraryDependencies ++= Seq(Apache.log4j % "provided", Apache.logging % "provided", logback % "provided", slf4j % "provided"))

  val sftpserver = OsgiProject("org.openmole.misc.sftpserver") dependsOn (tools) settings (libraryDependencies += Apache.sshd)

  val console = OsgiProject("org.openmole.misc.console", bundleActivator = Some("org.openmole.misc.console.Activator"), buddyPolicy = Some("global")) dependsOn
    (osgi /*, scalaCompiler*/ ) settings (includeOsgiProv, OsgiKeys.importPackage := Seq("*"), libraryDependencies += scalaLang)

}
