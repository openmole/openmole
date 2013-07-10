package root

import sbt._
import org.openmole.buildsystem.OMKeys._

object Gui extends GuiDefaults(gui.Misc, gui.Core, gui.Osgi, gui.Plugin) {
  override def dir = super.dir
}

abstract class GuiDefaults(subBuilds: Defaults*) extends Defaults(subBuilds: _*) {
  def dir = file("gui")
  override val org = "org.openmole.ide"
  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core"))
}

