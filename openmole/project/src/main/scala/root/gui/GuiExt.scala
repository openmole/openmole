package root.gui

import sbt._
import sbt.Keys._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._

object Ext extends GuiDefaults {
  override val dir = super.dir / "ext"

  lazy val data = OsgiProject("org.openmole.gui.ext") settings (
    libraryDependencies ++= Seq(scaladget)
  )
}