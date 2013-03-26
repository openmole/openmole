package projectRoot.core

import projectRoot.Libraries
import sbt._
import Keys._

trait CoreMisc extends CoreDefaults with Libraries {
  private implicit val dir = file("core/misc")

  lazy val coreMisc = Project("core-misc", dir) aggregate (coreMiscException, coreMiscMacros, coreMiscOsgi,
    coreMiscTools, coreMiscEventDispatcher, coreMiscFileDeleter, coreMiscFileCache, coreMiscFileService,
    coreMiscPluginManager, coreMiscReplication, coreMiscUpdater, coreMiscWorkspace, coreMiscHashService)

  lazy val coreMiscEventDispatcher = OsgiProject("org.openmole.misc.eventdispatcher") dependsOn(coreMiscTools)

  lazy val coreMiscException = OsgiProject("org.openmole.misc.exception")

  lazy val coreMiscHashService = OsgiProject("org.openmole.misc.hashservice") settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn
    (coreMiscException, gnuCrypto, coreMiscTools, apacheCommonsPool)

  lazy val coreMiscFileCache = OsgiProject("org.openmole.misc.filecache") settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn (coreMiscFileDeleter)

  lazy val coreMiscFileDeleter = OsgiProject("org.openmole.misc.filedeleter") settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn (coreMiscTools)

  lazy val coreMiscFileService = OsgiProject("org.openmole.misc.fileservice") settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn
    (coreMiscTools, coreMiscHashService, coreMiscFileCache, coreMiscUpdater, coreMiscWorkspace)

  lazy val coreMiscMacros = OsgiProject("org.openmole.misc.macros") dependsOn (scalaLang)

  lazy val coreMiscOsgi = OsgiProject("org.openmole.misc.osgi", buddyPolicy = Some("global"),
    bundleActivator = Some("org.openmole.misc.osgi.Activator")) dependsOn(coreMiscException, scalaLang) settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV})

  lazy val coreMiscTools = OsgiProject("org.openmole.misc.tools") settings
    (libraryDependencies <++= (osgiVersion, version) {(oV,v) => Seq("org.eclipse.core" % "org.eclipse.osgi" % oV,
      "org.openmole" % "com.ice.tar" % v)}) dependsOn //TODO sbtify ice.tar
    (coreMiscException, xstream, icu4j, xstream, icu4j, groovy, objenesis,
      apacheCommonsExec, apacheCommonsPool, apacheCommonsMath, coreMiscOsgi, jodaTime)

  lazy val coreMiscPluginManager = OsgiProject("org.openmole.misc.pluginmanager",
    bundleActivator = Some("org.openmole.misc.pluginmanager.internal.Activator")) settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn
    (coreMiscException, coreMiscTools, coreMiscOsgi)

  lazy val coreMiscReplication = OsgiProject("org.openmole.misc.replication") dependsOn(db4o, xstream)

  lazy val coreMiscUpdater = OsgiProject("org.openmole.misc.updater") dependsOn(coreMiscException, coreMiscTools, coreMiscWorkspace)

  lazy val coreMiscWorkspace = OsgiProject("org.openmole.misc.workspace") settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn
    (coreMiscException, coreMiscEventDispatcher, coreMiscTools, coreMiscReplication, jasypt, xstream, apacheCommonsConfig)

}