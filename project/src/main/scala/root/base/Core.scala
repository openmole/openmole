package root.base

import sbt._
import Keys._

package object core extends BaseDefaults {
  import misc._
  import root.ThirdParties._
  import root.libraries._

  implicit val dir = file("core/core")
  lazy val all = Project("core-core", dir) aggregate (model, serializer, implementation, batch)

  lazy val model = OsgiProject("org.openmole.core.model", openmoleScope = Some("provided")) dependsOn
    (eventDispatcher, exception, misc.tools, updater)

  lazy val serializer = OsgiProject("org.openmole.core.serializer", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn
    (model, workspace, xstream, pluginManager, hashService, fileService, misc.tools)

  lazy val implementation = OsgiProject("org.openmole.core.implementation", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) {oV => "org.eclipse.core" % "org.eclipse.osgi" % oV}) dependsOn
    (model, workspace, robustIt, exception, workspace, eventDispatcher,
      serializer, pluginManager)

  lazy val batch = OsgiProject("org.openmole.core.batch", openmoleScope = Some("provided"), imports = Seq("*")) dependsOn (implementation,
    workspace, misc.tools, eventDispatcher, replication, db4o, updater, serializer,
    fileService, hashService, pluginManager, iceTar, gridscale, guava)    //TODO: Finish adding gridscale and guava, then add to the aggregation

}