package root.base.plugin

import sbt._
import root.base._
import root.Libraries._

object Profiler extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.profiler")

  lazy val all = Aggregator("base-plugin-profiler") aggregate (csvprofiler)

  lazy val csvprofiler = OsgiProject("csvprofiler") dependsOn (provided(Misc.exception), Core.implementation, opencsv % "provided")
}