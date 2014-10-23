package root.gui

import org.openmole.buildsystem.OMKeys._

import sbt._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.libraries.Apache
import sbt.Keys._
import com.typesafe.sbt.osgi.OsgiKeys._

object Bootstrap extends GuiDefaults {
  override val dir = super.dir / "bootstrap"

  lazy val core = OsgiProject("org.openmole.gui.bootstrap") settings
    (libraryDependencies ++= Seq()) dependsOn
    (Server.core, Client.core, Client.dataui, Client.factoryui, Client.workflow, Shared.shared, Ext.data, base.Misc.pluginManager, base.Misc.workspace, base.Misc.tools, base.Misc.fileService)
}
