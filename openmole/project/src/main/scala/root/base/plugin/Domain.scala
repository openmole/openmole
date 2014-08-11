package root.base.plugin

import sbt._
import root.base._
import root.libraries.Apache

object Domain extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.domain")

  lazy val collection = OsgiProject("collection") dependsOn (Misc.exception, Core.implementation)

  lazy val distribution = OsgiProject("distribution") dependsOn (Misc.exception, Core.implementation, Misc.workspace)
  settings(libraryDependencies ++= Seq(Apache.math % "provided"))

  lazy val file = OsgiProject("file") dependsOn (Misc.exception, Core.implementation)

  lazy val modifier = OsgiProject("modifier") dependsOn (Misc.exception, Core.implementation, Tool.groovy)

  lazy val range = OsgiProject("range") dependsOn (Core.implementation, Misc.exception)

}
