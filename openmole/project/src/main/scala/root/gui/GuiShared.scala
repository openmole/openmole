package root.gui

import sbt._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._

object Shared extends GuiDefaults {
  override val dir = super.dir / "shared"

  lazy val shared = OsgiProject("org.openmole.gui.shared")
}