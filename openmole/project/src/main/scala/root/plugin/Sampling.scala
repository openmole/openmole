package plugin

import root.Libraries
import root.libraries._
import sbt._
import Keys._
import root.base._

object Sampling extends root.PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.sampling")

  lazy val combine = OsgiProject("combine", imports = Seq("*")) dependsOn (Misc.exception, Domain.modifier, Core.workflow, Tool.groovy)

  lazy val csv = OsgiProject("csv", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow, Tool.csv) settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val lhs = OsgiProject("lhs", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow, Misc.workspace)

  lazy val quasirandom = OsgiProject("quasirandom", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow, Misc.workspace) settings (
    libraryDependencies += Apache.math
  )
}
