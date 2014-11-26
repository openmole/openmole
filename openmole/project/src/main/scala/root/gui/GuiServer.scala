package root.gui

import org.openmole.buildsystem.OMKeys._

import sbt._
import root.{ GuiDefaults, base, Web }
import root.Libraries._
import root.libraries.Apache
import sbt.Keys._
import com.typesafe.sbt.osgi.OsgiKeys._
import root.gui.plugin.Task

object Server extends GuiDefaults {
  override val dir = super.dir / "server"

  lazy val core = OsgiProject("org.openmole.gui.server.core") settings
    (libraryDependencies ++= Seq(autowireJVM, upickleJVM, scalaTagsJVM, jetty, logback, scalatra)) dependsOn
    (Server.factory, Shared.shared, Ext.data, base.Core.model, base.Core.implementation)

  lazy val factory = OsgiProject("org.openmole.gui.server.factory") dependsOn
    (Ext.data, Ext.factoryui, Shared.shared, base.Core.model, base.Core.implementation)

  lazy val state = OsgiProject("org.openmole.gui.server.state") settings
    (libraryDependencies ++= Seq(slick)) dependsOn
    (Ext.data, base.Core.model, base.Core.implementation, base.Misc.workspace)
}
