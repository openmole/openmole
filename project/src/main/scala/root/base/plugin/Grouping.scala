package root.base.plugin

import sbt._
import root.base._

package object grouping extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.grouping")

  lazy val all = Project("base-plugin-grouping", dir) aggregate (batch, onvariable)

  lazy val batch = OsgiProject("batch") dependsOn (misc.exception, core.implementation, misc.workspace)

  lazy val onvariable = OsgiProject("onvariable") dependsOn (misc.exception, core.implementation)
}