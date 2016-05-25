package plugin

import root.Libraries
import sbt._
import Keys._
import root._

object Hook extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.hook")

  lazy val display = OsgiProject("display", imports = Seq("*")) dependsOn (Core.dsl)

  lazy val fileHook = OsgiProject("file", imports = Seq("*")) dependsOn (Core.dsl, Core.replication % "test") settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val modifier = OsgiProject("modifier", imports = Seq("*")) dependsOn (Core.dsl) settings (
    libraryDependencies += Libraries.scalatest
  )

}