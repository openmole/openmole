package root.base

import sbt._
import Keys._

import org.openmole.buildsystem.OMKeys._
import com.typesafe.sbt.osgi.OsgiKeys

object Misc extends BaseDefaults {
  import root.Libraries._
  import root.libraries.Apache
  import root.ThirdParties._

  override val dir = file("core/misc")

  lazy val all = Project("core-misc", dir) aggregate (exception, macros, osgi,
    tools, eventDispatcher, fileDeleter, fileCache, fileService,
    pluginManager, replication, updater, workspace, hashService, sftpserver, logging, console)

  val exception = OsgiProject("org.openmole.misc.exception")

  val osgi = OsgiProject("org.openmole.misc.osgi", buddyPolicy = Some("global"), imports = Seq("*"),
    bundleActivator = Some("org.openmole.misc.osgi.Activator")) dependsOn (provided(exception), provided(scalaLang), provided(scalaCompiler)) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" })

  val tools = OsgiProject("org.openmole.misc.tools", buddyPolicy = Some("global")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (provided(exception), xstream % "provided", icu4j % "provided", groovy, objenesis % "provided", Apache.exec,
      Apache.pool % "provided", Apache.math % "provided", osgi % "provided", jodaTime % "provided", iceTar, provided(scalaLang))

  val eventDispatcher = OsgiProject("org.openmole.misc.eventdispatcher") dependsOn (provided(tools))

  val replication = OsgiProject("org.openmole.misc.replication") dependsOn (db4o % "provided", xstream)

  val workspace = OsgiProject("org.openmole.misc.workspace") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(exception), provided(eventDispatcher), tools, provided(replication), jasypt % "provided", xstream % "provided", Apache.config % "provided",
      Apache.math % "provided")

  val hashService = OsgiProject("org.openmole.misc.hashservice") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(exception), gnuCrypto % "provided", provided(tools), Apache.pool % "provided")

  val fileDeleter = OsgiProject("org.openmole.misc.filedeleter") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(tools))

  val fileCache = OsgiProject("org.openmole.misc.filecache") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (fileDeleter)

  val macros = OsgiProject("org.openmole.misc.macros") dependsOn (provided(scalaLang), provided(scalaCompiler))

  val pluginManager = OsgiProject("org.openmole.misc.pluginmanager",
    bundleActivator = Some("org.openmole.misc.pluginmanager.internal.Activator")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(exception), provided(tools), osgi)

  val updater = OsgiProject("org.openmole.misc.updater") dependsOn (provided(exception), provided(tools),
    provided(workspace))

  val fileService = OsgiProject("org.openmole.misc.fileservice") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(tools), provided(hashService), fileCache, provided(updater), provided(workspace), iceTar % "provided")

  val logging = OsgiProject("org.openmole.misc.logging",
    bundleActivator = Some("org.openmole.misc.logging.internal.Activator")) dependsOn (provided(tools), provided(workspace),
      Apache.log4j % "provided", Apache.logging % "provided", logback % "provided", slf4j % "provided")

  val sftpserver = OsgiProject("org.openmole.misc.sftpserver") dependsOn
    (provided(tools), Apache.sshd)

  val console = OsgiProject("org.openmole.misc.console", bundleActivator = Some("org.openmole.misc.console.Activator"), buddyPolicy = Some("global")) dependsOn
    (scalaLang, osgi, scalaCompiler) settings (includeOsgi, OsgiKeys.importPackage := Seq("*"))

}