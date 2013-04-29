package root.base.plugin

import sbt._
import root.base._
import root.libraries._

package object profiler extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.profiler")

  lazy val all = Project("base-plugin-profiler", dir) aggregate (csvprofiler)

  lazy val csvprofiler = OsgiProject("csvprofiler") dependsOn (misc.exception, core.implementation, opencsv % "provided")
}