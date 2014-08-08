package root.gui

import org.openmole.buildsystem.OMKeys._

import sbt._
import root.{ GuiDefaults, base, Web }
import root.Libraries._
import root.libraries.Apache
import sbt.Keys._

object Server extends GuiDefaults {
  override val dir = super.dir / "server"

  lazy val server = OsgiProject("org.openmole.gui.server") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (scalatra, logback, jetty, scalajsDom, upickle, autowire, slick,
      Shared.shared, Ext.dataui,
      base.Misc.workspace)
}
