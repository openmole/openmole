package root.gui

import org.openmole.buildsystem.OMKeys._

import root.{ GuiDefaults, base }
import sbt._
import root.Libraries._

object Osgi extends GuiDefaults {
  override val dir = super.dir / "osgi"

  implicit val artifactPrefix = Some("org.openmole.ide.osgi")

  lazy val all = Project("gui-osgi", dir) aggregate (netlogo, netlogo4, netlogo5)

  lazy val netlogo = OsgiProject("netlogo") dependsOn (base.plugin.Task.netLogo)

  lazy val netlogo4 = OsgiProject("netlogo4") dependsOn (netlogo, base.plugin.Task.netLogo4, netlogo4_noscala)

  lazy val netlogo5 = OsgiProject("netlogo5") dependsOn (netlogo, base.plugin.Task.netLogo5, netlogo5_noscala)

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("guiPlugin"))

}