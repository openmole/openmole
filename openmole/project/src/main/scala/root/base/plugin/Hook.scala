package root.base.plugin

import sbt._
import Keys._
import root.base._
import root.Libraries._

object Hook extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.hook")

  lazy val display = OsgiProject("display") dependsOn (Misc.exception, Core.workflow, Misc.workspace)

  lazy val fileHook = OsgiProject("file") dependsOn (Misc.exception, Core.workflow, Misc.workspace, Core.serializer, Misc.replication % "test")

  lazy val modifier = OsgiProject("modifier") dependsOn (Core.workflow)

}