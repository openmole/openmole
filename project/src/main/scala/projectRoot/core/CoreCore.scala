package projectRoot.core

import sbt._
import Keys._

trait CoreCore extends CoreDefaults with CoreMisc {
  private implicit val dir = file("core/core")
  lazy val coreCore = Project("core-core", dir) aggregate (coreModel, coreSerializer, coreImpl)

  lazy val coreModel = OsgiProject("org.openmole.core.model", openmoleScope = Some("provided")) dependsOn
    (coreMiscEventDispatcher, coreMiscException, coreMiscTools, coreMiscUpdater)

  lazy val coreSerializer = OsgiProject("org.openmole.core.serializer", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn
    (coreModel, coreMiscWorkspace, xstream, coreMiscPluginManager, coreMiscHashService, coreMiscFileService, coreMiscTools)

  lazy val coreImpl = OsgiProject("org.openmole.core.implementation", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn
    (coreModel, coreMiscWorkspace, robustIt, coreMiscException, coreMiscWorkspace, coreMiscEventDispatcher,
      coreSerializer, coreMiscPluginManager)

}