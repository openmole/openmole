package root.gui

import sbt._
import sbt.Keys._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import java.io.File

object Client extends GuiDefaults {
  override val dir = super.dir / "client"

  lazy val service = OsgiProject("org.openmole.gui.client.service") settings (scalaJSSettings: _*) settings (
    libraryDependencies ++= Seq(autowire, upickle, rx, scalajsDom)) dependsOn
    (Ext.dataui, Ext.factoryui, Shared.shared, Misc.utils)

  lazy val core = OsgiProject("org.openmole.gui.client.core") dependsOn
    (Ext.factoryui, service, Shared.shared, Misc.utils, Misc.js) settings (
      libraryDependencies ++= Seq(autowire, upickle, scalaTags, rx, scalajsDom, scaladget)) settings (scalaJSSettings: _*)
}