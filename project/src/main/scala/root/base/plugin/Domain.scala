package root.base.plugin

import root.base.PluginDefaults
import sbt._


package object domain extends PluginDefaults {
  implicit val artifactPrefix = Some("")

  lazy val all = Project("core-plugin-domain", dir)
}