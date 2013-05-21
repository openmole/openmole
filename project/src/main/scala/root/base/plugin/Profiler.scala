package root.base.plugin

import sbt._
import root.base._
import root.libraries._

package object profiler extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.profiler")

  lazy val all = Aggregator("base-plugin-profiler") aggregate (csvprofiler)

  lazy val csvprofiler = OsgiProject("csvprofiler") dependsOn (provided(misc.exception), core.implementation, opencsv % "provided")
}