package root.base

import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object Core extends BaseDefaults {
  import Misc._
  import root.ThirdParties._
  import root.Libraries._
  import root.libraries.Apache

  override val dir = file("core/core")
  lazy val all = Project("core-core", dir) aggregate (model, serializer, implementation, batch) //TODO: Replace with aggregators

  lazy val model = OsgiProject("org.openmole.core.model", openmoleScope = Some("provided")) dependsOn
    (eventDispatcher, provided(exception), Misc.tools, provided(updater), provided(Misc.workspace))

  lazy val serializer = OsgiProject("org.openmole.core.serializer", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(model), provided(workspace), xstream, provided(pluginManager), provided(hashService), provided(fileService),
      provided(Misc.tools), provided(iceTar))

  lazy val implementation = OsgiProject("org.openmole.core.implementation", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (model, workspace, provided(robustIt), provided(exception), provided(eventDispatcher),
      provided(serializer), pluginManager, provided(icu4j), scalaLang, Apache.math, groovy) //TODO: THINGS REALLY DEPEND ON THESE LIBS. Deal with it

  lazy val batch = OsgiProject("org.openmole.core.batch", openmoleScope = Some("provided"), imports = Seq("*")) dependsOn (implementation,
    provided(workspace), provided(Misc.tools), provided(eventDispatcher), replication, db4o, provided(updater), provided(Misc.exception),
    serializer, jasypt, provided(fileService), provided(hashService), pluginManager, iceTar % "provided", gridscale,
    guava, Apache.config)

}