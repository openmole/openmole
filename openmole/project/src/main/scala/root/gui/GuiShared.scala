package root.gui

import sbt._
import root.{ Bin, Core, GuiDefaults }
import root.Libraries._

object Shared extends GuiDefaults {
  override val dir = super.dir / "shared"

  lazy val shared = OsgiProject("org.openmole.gui.shared") dependsOn (Ext.data, Bin.buildinfo)
}