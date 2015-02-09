package root.gui

import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import root.{ GuiDefaults, base }
import root.Libraries._
import java.io.File

object Client extends GuiDefaults {
  override val dir = super.dir / "client"

  lazy val service = OsgiProject("org.openmole.gui.client.service") settings (
    libraryDependencies ++= Seq(autowire, upickle, rx, scalajsDom)) enablePlugins (ScalaJSPlugin) dependsOn
    (Ext.dataui, Ext.factoryui, Shared.shared, Misc.utils)

  lazy val core = OsgiProject("org.openmole.gui.client.core") enablePlugins (ScalaJSPlugin) dependsOn
    (Ext.factoryui, service, Shared.shared, Misc.utils, Misc.js) settings (
      libraryDependencies ++= Seq(autowire, upickle, scalaTags, rx, scalajsDom, scaladget))
}