package root.gui

import sbt._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._

object Ext extends GuiDefaults {
  override val dir = super.dir / "ext"

  lazy val dataui = OsgiProject("org.openmole.gui.ext") dependsOn
    (Tools.tools)
}