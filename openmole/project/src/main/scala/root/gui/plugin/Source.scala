package root.gui.plugin

import sbt._
import Keys._
import root.gui._
import root.Libraries._

object Source extends GUIPluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.source")

  // lazy val file = OsgiProject("file") dependsOn (base.plugin.Source.file, base.Misc.replication % "test") settings (libraryDependencies += opencsv)
}
