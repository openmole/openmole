package root.base.plugin

import sbt._
import root.base._

package object hook extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.hook")

  lazy val all = Project("base-plugin-hook", dir) aggregate (display, file)

  lazy val display = OsgiProject("display") dependsOn (misc.exception, core.implementation, misc.workspace)

  lazy val file = OsgiProject("file") dependsOn (misc.exception, core.implementation, misc.workspace)
}