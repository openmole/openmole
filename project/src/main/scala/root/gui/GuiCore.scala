package root.gui

import sbt._
import root.base
import root.libraries._
import root.gui

object GuiCore extends GuiDefaults {
  override val dir = super.dir / "core"

  lazy val all = Project("gui-core", dir)

  lazy val model = OsgiProject("org.openmole.ide.core.model") dependsOn
    (base.core.model, base.misc.tools, xstream, apache.config, base.misc.exception)
}