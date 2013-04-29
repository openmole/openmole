package root.base.plugin

import sbt._
import root.base._
import root.libraries._

package object sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.sampling")

  lazy val all = Project("base-plugins-sampling", dir) aggregate (combine, csv, filter, lhs)

  lazy val combine = OsgiProject("combine") dependsOn (misc.exception, core.implementation, domain.modifier)

  lazy val csv = OsgiProject("csv") dependsOn (misc.exception, core.implementation, opencsv % "provided")

  lazy val filter = OsgiProject("filter") dependsOn (misc.exception, core.implementation, tools.groovy)

  lazy val lhs = OsgiProject("lhs") dependsOn (misc.exception, core.implementation)
}