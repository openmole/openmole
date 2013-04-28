package root.base

import sbt._
import Keys._

package object misc extends BaseDefaults {
  import root.libraries._
  import root.thirdparties._

  implicit val dir = file("core/misc")

  lazy val all = Project("core-misc", dir) aggregate (exception, macros, osgi,
    tools, eventDispatcher, fileDeleter, fileCache, fileService,
    pluginManager, replication, updater, workspace, hashService, sftpserver, logging)

  lazy val eventDispatcher = OsgiProject("org.openmole.misc.eventdispatcher") dependsOn (tools)

  lazy val exception = OsgiProject("org.openmole.misc.exception")

  lazy val hashService = OsgiProject("org.openmole.misc.hashservice") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (exception, gnuCrypto, tools, apache.pool)

  lazy val fileCache = OsgiProject("org.openmole.misc.filecache") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn (fileDeleter)

  lazy val fileDeleter = OsgiProject("org.openmole.misc.filedeleter") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn (tools)

  lazy val fileService = OsgiProject("org.openmole.misc.fileservice") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (tools, hashService, fileCache, updater, workspace)

  lazy val macros = OsgiProject("org.openmole.misc.macros") dependsOn (scalaLang)

  lazy val osgi = OsgiProject("org.openmole.misc.osgi", buddyPolicy = Some("global"),
    bundleActivator = Some("org.openmole.misc.osgi.Activator")) dependsOn (exception, scalaLang) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV })

  lazy val tools = OsgiProject("org.openmole.misc.tools", buddyPolicy = Some("global")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (exception, xstream, icu4j, xstream, icu4j, groovy, objenesis,
      apache.exec, apache.pool, apache.math, osgi, jodaTime, iceTar)

  lazy val pluginManager = OsgiProject("org.openmole.misc.pluginmanager",
    bundleActivator = Some("org.openmole.misc.pluginmanager.internal.Activator")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (exception, tools, osgi)

  lazy val replication = OsgiProject("org.openmole.misc.replication") dependsOn (db4o, xstream)

  lazy val updater = OsgiProject("org.openmole.misc.updater") dependsOn (exception, tools, workspace)

  lazy val workspace = OsgiProject("org.openmole.misc.workspace") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (exception, eventDispatcher, tools, replication, jasypt, xstream, apache.config)

  lazy val logging = OsgiProject("org.openmole.misc.logging",
    bundleActivator = Some("org.openmole.misc.logging.internal.Activator")) dependsOn (tools, workspace, apache.log4j,
      apache.logging, logback, slf4j)

  lazy val sftpserver = OsgiProject("org.openmole.misc.sftpserver") dependsOn
    (tools, apache.sshd)

}