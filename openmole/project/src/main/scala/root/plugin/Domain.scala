package plugin

import sbt._
import Keys._
import root._

object Domain extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.domain")

  lazy val collection = OsgiProject("collection", imports = Seq("*")) dependsOn (Core.exception, Core.workflow)

  lazy val distribution = OsgiProject("distribution", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, Core.workspace) settings
    (libraryDependencies ++= Seq(Libraries.math))

  lazy val fileDomain = OsgiProject("file", imports = Seq("*")) dependsOn (Core.exception, Core.workflow)

  lazy val modifier = OsgiProject("modifier", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, Tool.groovy) settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val range = OsgiProject("range", imports = Seq("*")) dependsOn (Core.workflow, Core.exception)

}
