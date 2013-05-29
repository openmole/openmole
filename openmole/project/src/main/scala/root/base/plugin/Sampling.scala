package root.base.plugin

import sbt._
import root.base._
import root.Libraries._

object Sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.sampling")

  lazy val all = Project("base-plugins-sampling", dir) aggregate (combine, csv, filter, lhs)

  lazy val combine = OsgiProject("combine") dependsOn (provided(Misc.exception), provided(Core.model), provided(Domain.modifier))

  lazy val csv = OsgiProject("csv") dependsOn (provided(Misc.exception), provided(Core.implementation), opencsv % "provided")

  lazy val filter = OsgiProject("filter") dependsOn (provided(Misc.exception), provided(Core.implementation), provided(Tools.groovy))

  lazy val lhs = OsgiProject("lhs") dependsOn (provided(Misc.exception), provided(Core.implementation), provided(Misc.workspace))
}