package root.gui

import sbt._
import root.base
import root.libraries._
import sbt.Keys._

package object core extends GuiDefaults {
  override val dir = super.dir / "core"

  lazy val all = Project("gui-core", dir) aggregate (model, implementation)

  lazy val model = OsgiProject("org.openmole.ide.core.model") dependsOn
    (provided(base.core.model), provided(base.misc.tools), provided(xstream), provided(apache.config), provided(apache.log4j),
      provided(netbeans), provided(groovy), base.misc.exception, provided(misc.widget))

  lazy val implementation = OsgiProject("org.openmole.ide.core.implementation") settings
    (libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }) dependsOn
    (provided(robustIt), model, provided(base.core.model), provided(base.core.batch), base.misc.exception, provided(base.misc.eventDispatcher),
      provided(base.misc.workspace), provided(base.misc.tools), provided(xstream), provided(apache.config), provided(apache.log4j), provided(groovy), provided(jodaTime), provided(netbeans),
      misc.widget, misc.tools, provided(misc.visualization), provided(gral))
}