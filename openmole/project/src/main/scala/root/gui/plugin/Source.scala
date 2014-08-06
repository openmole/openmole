package root.gui.plugin

import root.base
import sbt._
import root.gui._
import root.Libraries._

object Source extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.source")

  lazy val file = OsgiProject("file") dependsOn (Ext.dataui, base.plugin.Source.file, opencsv, base.Misc.replication % "test")
}
