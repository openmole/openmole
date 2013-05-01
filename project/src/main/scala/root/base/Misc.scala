package root.base

import sbt._
import Keys._

package object misc extends BaseDefaults {
  import root.libraries._
  import root.thirdparties._

  override val dir = file("core/misc")

  lazy val all = Project("core-misc", dir) aggregate (exception, macros, osgi,
    tools, eventDispatcher, fileDeleter, fileCache, fileService,
    pluginManager, replication, updater, workspace, hashService, sftpserver, logging)

  lazy val eventDispatcher = OsgiProject("org.openmole.misc.eventdispatcher") dependsOn (provided(tools))

  lazy val exception = OsgiProject("org.openmole.misc.exception")

  lazy val hashService = OsgiProject("org.openmole.misc.hashservice") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(exception), gnuCrypto % "provided", provided(tools), apache.pool % "provided")

  lazy val fileCache = OsgiProject("org.openmole.misc.filecache") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (fileDeleter)

  lazy val fileDeleter = OsgiProject("org.openmole.misc.filedeleter") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(tools))

  lazy val fileService = OsgiProject("org.openmole.misc.fileservice") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(tools), provided(hashService), fileCache, provided(updater), provided(workspace), iceTar % "provided")

  lazy val macros = OsgiProject("org.openmole.misc.macros") dependsOn (provided(scalaLang))

  lazy val osgi = OsgiProject("org.openmole.misc.osgi", buddyPolicy = Some("global"),
    bundleActivator = Some("org.openmole.misc.osgi.Activator")) dependsOn (provided(exception), provided(scalaLang)) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" })

  lazy val tools = OsgiProject("org.openmole.misc.tools", buddyPolicy = Some("global")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (provided(exception), xstream % "provided", icu4j % "provided", groovy, objenesis % "provided", apache.exec % "provided",
      apache.pool % "provided", apache.math % "provided", osgi % "provided", jodaTime % "provided", iceTar, provided(scalaLang))

  lazy val pluginManager = OsgiProject("org.openmole.misc.pluginmanager",
    bundleActivator = Some("org.openmole.misc.pluginmanager.internal.Activator")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(exception), provided(tools), provided(osgi))

  lazy val replication = OsgiProject("org.openmole.misc.replication") dependsOn (db4o % "provided", xstream)

  lazy val updater = OsgiProject("org.openmole.misc.updater") dependsOn (provided(exception), provided(tools),
    provided(workspace))

  lazy val workspace = OsgiProject("org.openmole.misc.workspace") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(exception), provided(eventDispatcher), tools, provided(replication), jasypt % "provided", xstream % "provided", apache.config % "provided",
      apache.math % "provided")

  lazy val logging = OsgiProject("org.openmole.misc.logging",
    bundleActivator = Some("org.openmole.misc.logging.internal.Activator")) dependsOn (provided(tools), provided(workspace),
      apache.log4j % "provided", apache.logging % "provided", logback % "provided", slf4j % "provided")

  lazy val sftpserver = OsgiProject("org.openmole.misc.sftpserver") dependsOn
    (provided(tools), apache.sshd)

}