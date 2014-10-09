package root.gui

import sbt._
import sbt.Keys._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._
import fr.iscpif.jsmanager.JSManagerPlugin._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Client extends GuiDefaults {
  override val dir = super.dir / "client"

  lazy val dataui = OsgiProject("org.openmole.gui.client.dataui") /*settings (jsManagerSettings: _*)*/ dependsOn
    (Ext.data) settings (
      libraryDependencies ++= Seq(scalaRxJS)
    //  jsCall := "Plot().run();",
    //  outputPath := Server.dir + "/src/main/webapp/"
    )

  lazy val factoryui = OsgiProject("org.openmole.gui.client.factoryui") dependsOn
    (dataui, Ext.data, base.Core.model, base.Core.model)

  lazy val workflow = OsgiProject("org.openmole.gui.client.workflow") settings (
    libraryDependencies ++= Seq(autowireJS, scalaTagsJS, scalaRxJS, scalajsDom, scaladget))

  lazy val client = OsgiProject("org.openmole.gui.client.client") settings (
    libraryDependencies ++= Seq(autowireJS, scalaTagsJS, scalaRxJS, scalajsDom, scaladget, upickleJS)) settings (jsManagerSettings: _*) //settings (scalaJSSettings: _*) ///settings (jsManagerSettings: _*) (scalaJSSettings: _*)
}