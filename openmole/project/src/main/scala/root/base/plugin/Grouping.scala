package root.base.plugin

import sbt._
import root.base._

object Grouping extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.grouping")

  lazy val all = Project("base-plugin-grouping", dir) aggregate (batch, onvariable)

  lazy val batch = OsgiProject("batch") dependsOn (Misc.exception, Core.implementation, Misc.workspace)

  lazy val onvariable = OsgiProject("onvariable") dependsOn (Misc.exception, Core.implementation)
}