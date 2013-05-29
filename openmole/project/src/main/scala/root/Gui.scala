package root

import root.gui.GuiDefaults
import sbt._

object Gui extends GuiDefaults {
  override def dir = super.dir
  lazy val all = Project("gui", dir) aggregate (gui.Misc.all, gui.Core.all, gui.Osgi.all, gui.Plugin.all)
}