package root.gui.plugin

import root.base
import sbt._
import root.gui._
import root.Libraries._

object Source extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.source")

  lazy val file = OsgiProject("file") dependsOn (Core.implementation, Miscellaneous.tools, base.plugin.Source.file, opencsv)
}