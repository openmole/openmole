package root.gui

import sbt._
import Keys._
import root.{ GuiDefaults, base }
import root.Libraries._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Tools extends GuiDefaults {
  override val dir = super.dir / "tools"

  lazy val tools = OsgiProject("org.openmole.gui.tools") settings (scalaJSSettings: _*) dependsOn
    (base.Misc.workspace) settings (
      libraryDependencies ++= Seq(scalajsLibrary, scalajsDom, scalaTagsJS, scalaRxJS)
    )
}