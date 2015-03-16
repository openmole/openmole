package root.gui

import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import Keys._
import root._
import root.Libraries._

object Misc extends GuiDefaults {
  override val dir = super.dir / "misc"

  lazy val utils = OsgiProject("org.openmole.gui.misc.utils") enablePlugins (ScalaJSPlugin)

  lazy val js = OsgiProject("org.openmole.gui.misc.js") enablePlugins (ScalaJSPlugin) dependsOn
    (Core.workspace, utils) settings (
      libraryDependencies ++= Seq(scalajsLibrary, scalajsDom, scalaTags, rx, scaladget)
    )
}