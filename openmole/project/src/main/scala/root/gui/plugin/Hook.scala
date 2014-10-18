package root.gui.plugin

import sbt._
import root.gui._
import root.base

object Hook extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.hook")

  lazy val display = OsgiProject("display") dependsOn (Core.implementation, Miscellaneous.tools, base.plugin.Hook.display, base.Misc.replication % "test")

  lazy val file = OsgiProject("file") dependsOn (Core.implementation, base.plugin.Hook.file, Miscellaneous.tools, base.Misc.replication % "test")
}