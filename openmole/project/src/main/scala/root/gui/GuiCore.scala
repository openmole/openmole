package root.gui

import org.openmole.buildsystem.OMKeys._

import sbt._
import root.{ GuiDefaults, base, Web }
import root.Libraries._
import root.libraries.Apache
import sbt.Keys._

object Core extends GuiDefaults {
  override val dir = super.dir / "core"

  lazy val implementation = OsgiProject("org.openmole.ide.core.implementation") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (robustIt, base.Core.model, base.Core.batch, base.Misc.exception, base.Misc.eventDispatcher, Web.misc,
      base.Misc.workspace, base.Misc.tools, xstream, Apache.config, Apache.log4j, groovy, jodaTime, netbeans,
      Misc.widget, Misc.tools, Misc.visualization, gral, base.Misc.replication, scalaz)
}
