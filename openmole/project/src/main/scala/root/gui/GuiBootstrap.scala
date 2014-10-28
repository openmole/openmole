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

  lazy val core = OsgiProject("org.openmole.gui.bootstrap") settings
    (libraryDependencies ++= Seq(scalajsLibrary, scalajsTools, scalajsDom, autowireJS, scalaTagsJS, scalaRxJS, upickleJS)) dependsOn
    (Server.core, Client.core, Client.dataui, Client.factoryui, Shared.shared, Ext.data, base.Misc.pluginManager, base.Misc.workspace, base.Misc.tools, base.Misc.fileService)
}
