package root.gui.plugin

import sbt._
import root.gui._
import root.gui
import root.base

package object hook extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.hook")

  lazy val all = Project("gui-plugin-hook", dir) aggregate (display, file)

  lazy val display = OsgiProject("display") dependsOn (core.implementation, misc.widget, miscellaneous.tools,
    base.plugin.hook.display)

  lazy val file = OsgiProject("file") dependsOn (core.implementation, misc.widget, base.plugin.hook.file, miscellaneous.tools)
}