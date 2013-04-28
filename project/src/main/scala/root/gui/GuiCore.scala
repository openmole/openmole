package root.gui

import sbt._
import root.base
import root.libraries._
import sbt.Keys._

package object core extends GuiDefaults {
  override val dir = super.dir / "core"

  lazy val all = Project("gui-core", dir) aggregate (model, implementation)

  lazy val model = OsgiProject("org.openmole.ide.core.model") dependsOn
    (base.core.model, base.misc.tools, xstream, apache.config, apache.log4j,
      netbeans, groovy, base.misc.exception, misc.widget)

  lazy val implementation = OsgiProject("org.openmole.ide.core.implementation") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (robustIt, model, base.core.model, base.core.batch, base.misc.exception, base.misc.eventDispatcher,
      base.misc.workspace, base.misc.tools, xstream, apache.config, apache.log4j, groovy, jodaTime, netbeans,
      misc.widget, misc.tools, misc.visualization, gral)
}