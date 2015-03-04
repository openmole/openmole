package root.gui

import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import root.{ GuiDefaults }
import root.Libraries._
import java.io.File

object Client extends GuiDefaults {
  override val dir = super.dir / "client"

  lazy val core = OsgiProject("org.openmole.gui.client.core") enablePlugins (ScalaJSPlugin) dependsOn
    (Ext.dataui, Shared.shared, Misc.utils, Misc.js) settings (
      libraryDependencies ++= Seq(autowire, upickle, scalaTags, rx, scalajsDom, scaladget))
}