package root.gui

import org.openmole.buildsystem.OMKeys._

import sbt._
import root.{ GuiDefaults, base, Web }
import root.Libraries._
import root.libraries.Apache
import sbt.Keys._
import com.typesafe.sbt.osgi.OsgiKeys._
import root.gui.plugin.Task
//import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Server extends GuiDefaults {
  override val dir = super.dir / "server"

  lazy val core = OsgiProject("org.openmole.gui.server.core") settings
    (libraryDependencies ++= Seq(scalaTagsJS, jetty, logback, scalatra, scalajsDom, upickleJVM, autowireJVM, scalajsLibrary, scalajsTools)) dependsOn
    (Server.factory, Shared.shared, Ext.data, base.Core.model, base.Core.implementation, base.Misc.pluginManager)
  /*settings (bundle <<= bundle dependsOn (
       sbt.Keys.`package` in Client.client in Compile, sbt.Keys.`package` in Task.groovyExt in Compile)) settings (scalaJSSettings: _*)*/

  lazy val factory = OsgiProject("org.openmole.gui.server.factory") settings
    (includeOsgi) dependsOn
    (Ext.data, base.Core.model, base.Core.implementation)

  lazy val state = OsgiProject("org.openmole.gui.server.state") settings
    (includeOsgi, libraryDependencies ++= Seq(slick)) dependsOn
    (Ext.data, base.Core.model, base.Core.implementation, base.Misc.workspace)
}
