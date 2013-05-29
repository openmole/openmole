package root.gui

import sbt._
import root.base
import root.Libraries._
import root.ThirdParties._

object Misc extends GuiDefaults {
  override val dir = super.dir / "misc"

  lazy val all = Project("gui-misc", dir) aggregate (tools, widget, visualization)

  lazy val tools = OsgiProject("org.openmole.ide.misc.tools") dependsOn
    (base.Core.implementation, provided(base.Misc.workspace))

  lazy val widget = OsgiProject("org.openmole.ide.misc.widget") dependsOn
    (base.Misc.workspace, base.Core.model, tools, jsyntaxpane,
      miglayout, netbeans, scalaSwing, base.Misc.exception)

  lazy val visualization = OsgiProject("org.openmole.ide.misc.visualization") dependsOn
    (base.Core.model, widget, provided(gral))
}