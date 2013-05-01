package root.base.plugin

import sbt._
import root.base._
import root.libraries._

package object sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.sampling")

  lazy val all = Project("base-plugins-sampling", dir) aggregate (combine, csv, filter, lhs)

  lazy val combine = OsgiProject("combine") dependsOn (provided(misc.exception), provided(core.model), provided(domain.modifier))

  lazy val csv = OsgiProject("csv") dependsOn (provided(misc.exception), provided(core.implementation), opencsv % "provided")

  lazy val filter = OsgiProject("filter") dependsOn (provided(misc.exception), provided(core.implementation), provided(tools.groovy))

  lazy val lhs = OsgiProject("lhs") dependsOn (provided(misc.exception), provided(core.implementation), provided(misc.workspace))
}