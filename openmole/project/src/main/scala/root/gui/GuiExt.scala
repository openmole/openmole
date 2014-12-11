package root.gui

import sbt._
import sbt.Keys._
import root.GuiDefaults
import root.Libraries._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Ext extends GuiDefaults {
  override val dir = super.dir / "ext"

  lazy val aspects = OsgiProject("org.openmole.gui.ext.aspects") settings (scalaJSSettings: _*)

  lazy val data = OsgiProject("org.openmole.gui.ext.data") settings (scalaJSSettings: _*)

  lazy val dataui: Project = OsgiProject("org.openmole.gui.ext.dataui") dependsOn (aspects, data) settings (scalaJSSettings: _*) settings (
    libraryDependencies ++= Seq(rx, scalaTags, scalajsDom)
  )

  lazy val factoryui = OsgiProject("org.openmole.gui.ext.factoryui") settings (scalaJSSettings: _*) dependsOn (aspects, dataui)
}