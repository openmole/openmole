package root.gui

import sbt._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._
//import fr.iscpif.jsmanager.JSManagerPlugin._

object Shared extends GuiDefaults {
  override val dir = super.dir / "shared"

  lazy val shared = OsgiProject("org.openmole.gui.shared") //settings (jsManagerSettings: _*)
}