package root.gui.plugin

import root.base
import sbt._
import root.gui._
import root._

object Task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.task")

  lazy val exploration = OsgiProject("exploration") dependsOn (Ext.dataui, base.Core.model, base.Misc.exception, base.Misc.replication % "test")

  lazy val groovy = OsgiProject("groovy") dependsOn (Ext.dataui, base.Misc.workspace, base.plugin.Task.groovy, base.Misc.replication % "test")

  // lazy val imagej = OsgiProject("imagej") dependsOn (Ext.dataui, base.Misc.workspace, base.plugin.Task.groovy, base.Misc.replication % "test")

  lazy val template = OsgiProject("template") dependsOn (Ext.dataui, base.Misc.workspace, base.plugin.Task.template, base.Misc.replication % "test")

  lazy val moletask = OsgiProject("moletask") dependsOn (Ext.dataui, base.Core.model, base.Misc.replication % "test")

  lazy val netlogo = OsgiProject("netlogo") dependsOn (Ext.dataui, base.Core.model,
    provided(base.plugin.Task.netLogo4), provided(base.plugin.Task.netLogo5), base.Misc.replication % "test", base.plugin.Tool.netLogo4API, base.plugin.Tool.netLogo5API)

  lazy val stat = OsgiProject("stat") dependsOn (Ext.dataui, base.plugin.Task.stat, base.Core.model, base.Misc.replication % "test")

  lazy val tools = OsgiProject("tools") dependsOn (Ext.dataui, base.plugin.Task.tools, base.Core.model, base.Misc.replication % "test")

  lazy val systemexec = OsgiProject("systemexec") dependsOn (Ext.dataui, base.plugin.Task.systemexec,
    base.Core.model)

}
