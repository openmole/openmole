package root.gui

import sbt._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._

object Tools extends GuiDefaults {
  override val dir = super.dir / "tools"

  lazy val tools = OsgiProject("org.openmole.gui.tools") dependsOn
    (autowire, scalaTags, scalaRx, scalajsDom, provided(base.Misc.workspace))
}