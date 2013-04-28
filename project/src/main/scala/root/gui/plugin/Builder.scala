package root.gui.plugin

import sbt._
import root.gui._

package object builder extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.builder")

  lazy val all = Project("gui-plugin-builder", dir) aggregate (base)

  lazy val base = OsgiProject("base") dependsOn (core.implementation, misc.widget, root.base.plugin.builder.base)
}