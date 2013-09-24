package root.base

import root.BaseDefaults
import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object Core extends BaseDefaults {
  import Misc._
  import root.ThirdParties._
  import root.Libraries._
  import root.libraries.Apache

  implicit val artifactPrefix = Some("org.openmole.core")

  override val dir = file("core/core")

  lazy val model = OsgiProject("model", openmoleScope = Some("provided")) dependsOn
    (eventDispatcher, provided(exception), Misc.tools, provided(updater), provided(Misc.workspace))

  lazy val serializer = OsgiProject("serializer", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (provided(model), provided(workspace), xstream, provided(pluginManager), provided(hashService), provided(fileService),
      provided(Misc.tools), provided(iceTar))

  lazy val implementation = OsgiProject("implementation", openmoleScope = Some("provided")) settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV % "provided" }) dependsOn
    (model, workspace, provided(robustIt), provided(exception), provided(eventDispatcher),
      provided(serializer), pluginManager, scalaLang, Apache.math, groovy, Misc.hashService % "test", Misc.replication % "test") //TODO: THINGS REALLY DEPEND ON THESE LIBS. Deal with it

  lazy val batch = OsgiProject("batch", openmoleScope = Some("provided"), imports = Seq("*")) dependsOn (implementation,
    provided(workspace), provided(Misc.tools), provided(eventDispatcher), replication, db4o, provided(updater), provided(Misc.exception),
    serializer, jasypt, provided(fileService), provided(hashService), pluginManager, iceTar % "provided",
    guava, Apache.config) settings (includeGridscale)

  lazy val convenience = OsgiProject("convenience", openmoleScope = Some("provided"), buddyPolicy = Some("global")) dependsOn (implementation, scalaLang, scalaCompiler, Misc.macros)

}
