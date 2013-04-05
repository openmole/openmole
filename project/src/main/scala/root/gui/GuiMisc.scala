package root.gui

import sbt._
import root.{base, gui}


package object misc extends GuiDefaults {
  override val dir = super.dir / "misc"

  lazy val all = Project("gui-misc", dir) aggregate(tools)

  lazy val tools = OsgiProject("org.openmole.ide.misc.tools") dependsOn
    (base.core.implementation)
}