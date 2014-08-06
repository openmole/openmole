package root.gui.plugin

import root.base
import sbt._
import root.gui._

object Domain extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.domain")

  lazy val collection = OsgiProject("collection") dependsOn (Ext.dataui, base.plugin.Domain.collection, base.Misc.replication % "test")

  lazy val distribution = OsgiProject("distribution") dependsOn (Ext.dataui, base.plugin.Domain.distribution,
    base.plugin.Domain.modifier, base.Misc.replication % "test")

  lazy val file = OsgiProject("file") dependsOn (Ext.dataui, base.plugin.Domain.file, base.Misc.replication % "test")

  lazy val modifier = OsgiProject("modifier") dependsOn (Ext.dataui, base.plugin.Domain.modifier, base.Misc.replication % "test")

  lazy val range = OsgiProject("range") dependsOn (Ext.dataui, base.plugin.Domain.range, base.Misc.replication % "test")
}
