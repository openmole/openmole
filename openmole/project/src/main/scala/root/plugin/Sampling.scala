package plugin

import root._
import root.Libraries
import sbt._
import Keys._

object Sampling extends root.PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.sampling")

  lazy val combine = OsgiProject("combine", imports = Seq("*")) dependsOn (Core.exception, Domain.modifier, Core.workflow, Tool.groovy)

  lazy val csv = OsgiProject("csv", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, Tool.csv) settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val lhs = OsgiProject("lhs", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, Core.workspace)

  lazy val quasirandom = OsgiProject("quasirandom", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, Core.workspace) settings (
    libraryDependencies += Libraries.math
  )
}
