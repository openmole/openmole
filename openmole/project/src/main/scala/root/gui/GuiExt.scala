package root.gui

import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import root.GuiDefaults
import root.Libraries._

object Ext extends GuiDefaults {
  override val dir = super.dir / "ext"

  lazy val data = OsgiProject("org.openmole.gui.ext.data") enablePlugins (ScalaJSPlugin)

  lazy val dataui: Project = OsgiProject("org.openmole.gui.ext.dataui") dependsOn (data, Misc.js) enablePlugins (ScalaJSPlugin) settings (
    libraryDependencies ++= Seq(rx, scalaTags, scalajsDom)
  )

  lazy val factoryui = OsgiProject("org.openmole.gui.ext.factoryui") enablePlugins (ScalaJSPlugin) dependsOn (dataui)
}