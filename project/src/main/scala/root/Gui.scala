package root

import sbt._

package object gui extends GuiDefaults {
  override def dir = super.dir
  lazy val all = Project("gui", dir) aggregate (gui.misc.all, gui.core.all)
}