package root.gui.plugin

import root.base
import sbt._
import root.gui._

package object task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.task")

  lazy val all = Project("gui-plugin-task", dir) aggregate (exploration, groovy, moletask, netlogo, stat, systemexec)

  lazy val exploration = OsgiProject("exploration") dependsOn (core.implementation, base.core.implementation,
    core.model, misc.tools, base.misc.exception, misc.widget)

  lazy val groovy = OsgiProject("groovy") dependsOn (core.implementation, base.misc.workspace, base.plugin.task.groovy)

  lazy val moletask = OsgiProject("moletask") dependsOn (core.implementation, base.core.implementation)

  lazy val netlogo = OsgiProject("netlogo") dependsOn (core.implementation, base.core.implementation, osgi.netlogo4,
    base.plugin.task.netLogo4, base.core.implementation, base.plugin.task.netLogo5, base.misc.workspace, osgi.netlogo5)

  lazy val stat = OsgiProject("stat") dependsOn (core.implementation, base.plugin.task.stat, base.core.implementation)

  lazy val systemexec = OsgiProject("systemexec") dependsOn (core.implementation, base.plugin.task.systemexec,
    base.core.implementation)

}