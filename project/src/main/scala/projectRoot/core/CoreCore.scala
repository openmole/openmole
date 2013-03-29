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

  lazy val coreBatch = OsgiProject("org.openmole.core.batch", openmoleScope = Some("provided")) dependsOn (coreImpl,
    coreMiscWorkspace, coreMiscTools, coreMiscEventDispatcher, coreMiscReplication, db4o, coreMiscUpdater, coreSerializer,
    coreMiscFileService, coreMiscHashService, coreMiscPluginManager, iceTar, gridscale, guava)    //TODO: Finish adding gridscale and guava, then add to the aggregation

}