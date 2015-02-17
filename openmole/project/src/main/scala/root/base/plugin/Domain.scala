package root.base.plugin

import root.Libraries
import sbt._
import Keys._
import root.base._
import root.libraries.Apache

object Domain extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.domain")

  lazy val collection = OsgiProject("collection", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow)

  lazy val distribution = OsgiProject("distribution", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow, Misc.workspace) settings
    (libraryDependencies ++= Seq(Apache.math))

  lazy val fileDomain = OsgiProject("file", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow)

  lazy val modifier = OsgiProject("modifier", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow, Tool.groovy) settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val range = OsgiProject("range", imports = Seq("*")) dependsOn (Core.workflow, Misc.exception)

}
