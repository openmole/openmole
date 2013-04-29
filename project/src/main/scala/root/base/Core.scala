package root.base

import sbt._
import Keys._

package object core extends BaseDefaults {
  import misc._
  import root.thirdparties._
  import root.libraries._

  override val dir = file("core/core")
  lazy val all = Project("core-core", dir) aggregate (model, serializer, implementation, batch)

  lazy val model = OsgiProject("org.openmole.core.model", openmoleScope = Some("provided")) dependsOn
    (provided(eventDispatcher), provided(exception), provided(misc.tools), provided(updater))

  lazy val serializer = OsgiProject("org.openmole.core.serializer", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (model, workspace, xstream % "provided", pluginManager, hashService, fileService, misc.tools, iceTar % "provided")

  lazy val implementation = OsgiProject("org.openmole.core.implementation", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (model, workspace, provided(robustIt), exception, provided(eventDispatcher),
      provided(serializer), pluginManager, provided(icu4j), scalaLang, apache.math, groovy) //TODO: THINGS REALLY DEPEND ON THESE LIBS. Deal with it

  lazy val batch = OsgiProject("org.openmole.core.batch", openmoleScope = Some("provided"), imports = Seq("*")) dependsOn (implementation,
    provided(workspace), provided(misc.tools), provided(eventDispatcher), replication, db4o, updater, serializer, jasypt,
    fileService, hashService, pluginManager, iceTar % "provided", gridscale % "provided", guava, apache.config)

}