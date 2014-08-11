package root.gui

import sbt._
import root.{GuiDefaults, base}
import root.Libraries._
import root.ThirdParties._

object Tools extends GuiDefaults {
  override val dir = super.dir / "tools"

  lazy val tools = OsgiProject("org.openmole.gui.tools") dependsOn
    (provided(base.Misc.workspace)) settings (
    libraryDependencies ++= Seq(autowire, scalaTags, scalaRx, scalajsDom)
    )
}