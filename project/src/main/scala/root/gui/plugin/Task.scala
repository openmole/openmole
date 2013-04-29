package root.gui.plugin

import root.base
import sbt._
import root.gui._

package object task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.task")

  lazy val all = Project("gui-plugin-task", dir) aggregate (exploration, groovy, moletask, netlogo, stat, systemexec)

  lazy val exploration = OsgiProject("exploration") dependsOn (core.implementation, base.core.model, base.misc.exception)

  lazy val groovy = OsgiProject("groovy") dependsOn (core.implementation, base.misc.workspace, base.plugin.task.groovy)

  lazy val moletask = OsgiProject("moletask") dependsOn (core.implementation, base.core.model)

  lazy val netlogo = OsgiProject("netlogo") dependsOn (core.implementation, base.core.model, osgi.netlogo4,
    base.plugin.task.netLogo4, base.plugin.task.netLogo5, osgi.netlogo5)

  lazy val stat = OsgiProject("stat") dependsOn (core.implementation, base.plugin.task.stat, base.core.model)

  lazy val systemexec = OsgiProject("systemexec") dependsOn (core.implementation, base.plugin.task.systemexec,
    base.core.model)

}