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
    (provided(robustIt), provided(base.Core.model), provided(base.Core.batch), base.Misc.exception, provided(base.Misc.eventDispatcher), provided(Web.misc),
      base.Misc.workspace, provided(base.Misc.tools), provided(xstream), provided(Apache.config), provided(Apache.log4j), groovy, provided(jodaTime), provided(netbeans),
      Misc.widget, Misc.tools, provided(Misc.visualization), provided(gral), scalaz, base.Misc.replication % "test")
}
