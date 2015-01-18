package root.base.plugin

import sbt._
import Keys._
import root.base._
import root.libraries.Apache

object Domain extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.domain")

  lazy val collection = OsgiProject("collection") dependsOn (Misc.exception, Core.workflow)

  lazy val distribution = OsgiProject("distribution") dependsOn (Misc.exception, Core.workflow, Misc.workspace) settings
    (libraryDependencies ++= Seq(Apache.math % "provided"))

  lazy val fileDomain = OsgiProject("file") dependsOn (Misc.exception, Core.workflow)

  lazy val modifier = OsgiProject("modifier") dependsOn (Misc.exception, Core.workflow, Tool.groovy)

  lazy val range = OsgiProject("range") dependsOn (Core.workflow, Misc.exception)

}
