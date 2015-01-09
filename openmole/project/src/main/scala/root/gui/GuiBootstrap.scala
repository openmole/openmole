package root.gui

import org.openmole.buildsystem.OMKeys._

import sbt._
import root.{ GuiDefaults, base }
import root.Libraries._
import sbt.Keys._
import com.typesafe.sbt.osgi.OsgiKeys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Bootstrap extends GuiDefaults {
  override val dir = super.dir / "bootstrap"

  lazy val js = OsgiProject("org.openmole.gui.bootstrap.js") settings
    (libraryDependencies ++= Seq(scalajsTools, scalajsDom, autowire, scalaTags, rx, upickle)) dependsOn
    (Server.core, Client.core, base.Misc.pluginManager, base.Misc.workspace, base.Misc.tools, base.Misc.fileService)

  lazy val osgi = OsgiProject("org.openmole.gui.bootstrap.osgi") dependsOn
    (Server.factory, Client.service, Ext.data, Ext.factoryui, base.Core.model)
}