package root.gui

import sbt._
import sbt.Keys._
import root.GuiDefaults
import root.Libraries._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Ext extends GuiDefaults {
  override val dir = super.dir / "ext"

  lazy val data = OsgiProject("org.openmole.gui.ext.data") settings (scalaJSSettings: _*)

  lazy val dataui = OsgiProject("org.openmole.gui.ext.dataui") dependsOn (data) settings (scalaJSSettings: _*) settings (
    libraryDependencies ++= Seq(scalaRxJS)
  )

  lazy val factoryui = OsgiProject("org.openmole.gui.ext.factoryui") settings (scalaJSSettings: _*) dependsOn (dataui)
}