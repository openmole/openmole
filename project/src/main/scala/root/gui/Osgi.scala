package root.gui

import root.base
import sbt._


package object osgi extends GuiDefaults {
  override val dir = super.dir / "osgi"

  lazy val all = Project("gui-osgi", dir)

  lazy val netlogo = OsgiProject("org.openmole.ide.osgi.netlogo") dependsOn (base.plugin.task.netLogo)
}