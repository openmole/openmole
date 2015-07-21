package root

import sbt._
import org.openmole.buildsystem.OMKeys._

object Gui extends GuiDefaults(gui.Bootstrap, gui.Client, gui.Ext, gui.Misc, gui.Server, gui.Shared, gui.Plugin) {
  override def dir = super.dir

  lazy val all = Project("gui", dir) aggregate (subProjects: _*) //TODO: Quick hack to workaround the file hungriness of SBT 0.13.0 fix when https://github.com/sbt/sbt/issues/937 is fixed

}

abstract class GuiDefaults(subBuilds: Defaults*) extends Defaults(subBuilds: _*) {
  def dir = file("gui")
  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set("gui"))
}

