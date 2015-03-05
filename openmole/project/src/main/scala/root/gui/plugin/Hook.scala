package root.gui.plugin

import sbt._
import root.gui._

object Hook extends GUIPluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.hook")

  // lazy val display = OsgiProject("display") dependsOn (base.plugin.Hook.display, base.Misc.replication % "test")

  // lazy val file = OsgiProject("file") dependsOn (base.plugin.Hook.file, base.Misc.replication % "test")
}
