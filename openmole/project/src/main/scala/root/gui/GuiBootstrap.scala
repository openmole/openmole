package root.gui

import org.openmole.buildsystem.OMKeys._

import sbt._
import root._
import root.Libraries._
import sbt.Keys._
import com.typesafe.sbt.osgi.OsgiKeys._

object Bootstrap extends GuiDefaults {
  override val dir = super.dir / "bootstrap"

  lazy val js = OsgiProject("org.openmole.gui.bootstrap.js") settings
    (libraryDependencies ++= Seq(scalajsTools, scalajsDom, autowire, scalaTags, rx, upickle)) dependsOn
    (Server.core, Client.core, Core.pluginManager, Core.workspace, Core.tools, Core.fileService)

  lazy val osgi = OsgiProject("org.openmole.gui.bootstrap.osgi") dependsOn
    (Server.core, Client.core, Ext.data, Core.workflow)
}