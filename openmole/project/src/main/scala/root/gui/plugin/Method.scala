package root.gui.plugin

import root.base
import sbt._
import root.gui._

object Method extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.method")

  lazy val sensitivity = OsgiProject("sensitivity") dependsOn (base.plugin.Method.sensitivity, Domain.range,
    Core.implementation, base.Misc.replication % "test")
}