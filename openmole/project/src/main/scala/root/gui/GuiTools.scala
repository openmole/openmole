package root.gui

import sbt._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._

object Tools extends GuiDefaults {
  override val dir = super.dir / "tools"

  lazy val js = OsgiProject("org.openmole.gui.tools.js") dependsOn
    (autowire, scalaTags, scalaRx, scalajsDom, scalajsJQuery, provided(base.Misc.workspace))
}