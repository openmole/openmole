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
    (scalatra, logback, jetty, scalajsDom, autowire, slick,
      Shared.shared, Ext.dataui,
      base.Misc.workspace)

  /*   (robustIt, base.Core.model, base.Core.batch, base.Misc.exception, base.Misc.eventDispatcher, Web.misc,
      base.Misc.workspace, base.Misc.tools, xstream, Apache.config, Apache.log4j, groovy, jodaTime, netbeans,
      Misc.widget, Misc.tools, Misc.visualization, gral, scalaz, base.Misc.replication)*/
}
