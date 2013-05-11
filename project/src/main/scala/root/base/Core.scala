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
    (provided(eventDispatcher), provided(exception), misc.tools, provided(updater), provided(misc.workspace))

  lazy val serializer = OsgiProject("org.openmole.core.serializer", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(model), provided(workspace), xstream, provided(pluginManager), provided(hashService), provided(fileService),
      provided(misc.tools), provided(iceTar))

  lazy val implementation = OsgiProject("org.openmole.core.implementation", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (model, workspace, provided(robustIt), provided(exception), provided(eventDispatcher),
      provided(serializer), pluginManager, provided(icu4j), scalaLang, apache.math, groovy) //TODO: THINGS REALLY DEPEND ON THESE LIBS. Deal with it

  lazy val batch = OsgiProject("org.openmole.core.batch", openmoleScope = Some("provided"), imports = Seq("*")) dependsOn (implementation,
    provided(workspace), provided(misc.tools), provided(eventDispatcher), replication, db4o, provided(updater), provided(misc.exception),
    serializer, jasypt, provided(fileService), provided(hashService), pluginManager, iceTar % "provided", gridscale,
    guava, apache.config)

}