package root.gui.plugin

import root.base
import sbt._
import root.gui._
import root._

object Task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.task")

  lazy val exploration = OsgiProject("exploration") dependsOn (Core.implementation, base.Core.model, base.Misc.exception, base.Misc.replication % "test")

  lazy val groovy = OsgiProject("groovy") dependsOn (Core.implementation, base.Misc.workspace, base.plugin.Task.groovy, base.Misc.replication % "test")

  // lazy val imagej = OsgiProject("imagej") dependsOn (Core.implementation, base.Misc.workspace, base.plugin.Task.groovy, base.Misc.replication % "test")

  lazy val template = OsgiProject("template") dependsOn (Core.implementation, base.Misc.workspace, base.plugin.Task.template, base.Misc.replication % "test")

  lazy val moletask = OsgiProject("moletask") dependsOn (Core.implementation, base.Core.model, base.Misc.replication % "test")

  lazy val netlogo = OsgiProject("netlogo") dependsOn (Core.implementation, base.Core.model, Miscellaneous.tools,
    provided(base.plugin.Task.netLogo4), provided(base.plugin.Task.netLogo5), base.Misc.replication % "test", base.plugin.Tool.netLogo4API, base.plugin.Tool.netLogo5API)

  lazy val stat = OsgiProject("stat") dependsOn (Core.implementation, base.plugin.Task.statistics, base.Core.model, base.Misc.replication % "test")

  lazy val tools = OsgiProject("tools") dependsOn (Core.implementation, base.plugin.Task.tools, base.Core.model, base.Misc.replication % "test")

  lazy val systemexec = OsgiProject("systemexec") dependsOn (Core.implementation, base.plugin.Task.systemexec,
    base.Core.model, Miscellaneous.tools)

}
