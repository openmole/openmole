package root.gui

import sbt._
import Keys._
import root.{ GuiDefaults, base }
import root.Libraries._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Misc extends GuiDefaults {
  override val dir = super.dir / "misc"

  lazy val utils = OsgiProject("org.openmole.gui.misc.utils")

  lazy val js = OsgiProject("org.openmole.gui.misc.js") settings (scalaJSSettings: _*) dependsOn
    (base.Misc.workspace) settings (
      libraryDependencies ++= Seq(scalajsLibrary, scalajsDom, scalaTags, rx, scaladget)
    )
}