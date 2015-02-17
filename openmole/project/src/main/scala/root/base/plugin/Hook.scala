package root.base.plugin

import root.Libraries
import sbt._
import Keys._
import root.base._

object Hook extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.hook")

  lazy val display = OsgiProject("display", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow, Misc.workspace)

  lazy val fileHook = OsgiProject("file", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow, Misc.workspace, Core.serializer, Misc.replication % "test") settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val modifier = OsgiProject("modifier", imports = Seq("*")) dependsOn (Core.workflow) settings (
    libraryDependencies += Libraries.scalatest
  )

}