package root.gui

import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import root._
import root.Libraries._

object Ext extends GuiDefaults {
  override val dir = super.dir / "ext"

  lazy val data = OsgiProject("org.openmole.gui.ext.data") enablePlugins (ScalaJSPlugin) dependsOn (Core.workflow)

  lazy val dataui: Project = OsgiProject("org.openmole.gui.ext.dataui") dependsOn (data, Misc.js) enablePlugins (ScalaJSPlugin) settings (
    libraryDependencies ++= Seq(rx, scalaTags, scalajsDom)
  )
}