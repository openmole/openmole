package root.gui

import org.openmole.buildsystem.OMKeys._

import sbt._
import root.{GuiDefaults, base, Web}
import root.Libraries._
import root.libraries.Apache
import sbt.Keys._
import com.typesafe.sbt.osgi.OsgiKeys._
import fr.iscpif.jsmanager.JSManagerPlugin._

object Server extends GuiDefaults {
  override val dir = super.dir / "server"

  lazy val server = OsgiProject("org.openmole.gui.server") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV},
      libraryDependencies ++= Seq(scalatra, logback, jetty, scalajsDom, upickle, autowire, slick)) dependsOn
    (Shared.shared, Ext.dataui,base.Misc.workspace) settings (bundle <<= bundle dependsOn (toJs in Client.client))
}
