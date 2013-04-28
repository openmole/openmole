package root.gui

import root.base
import sbt._

package object osgi extends GuiDefaults {
  override val dir = super.dir / "osgi"

  implicit val artifactPrefix = Some("org.openmole.ide.osgi")

  lazy val all = Project("gui-osgi", dir) aggregate (netlogo, netlogo4, netlogo5)

  lazy val netlogo = OsgiProject("netlogo") dependsOn (base.plugin.task.netLogo)

  lazy val netlogo4 = OsgiProject("netlogo4") dependsOn (netlogo, base.plugin.task.netLogo4)

  lazy val netlogo5 = OsgiProject("netlogo5") dependsOn (netlogo, base.plugin.task.netLogo5)
}