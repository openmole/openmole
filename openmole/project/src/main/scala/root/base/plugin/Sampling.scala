package root.base.plugin

import root.Libraries
import root.libraries._
import sbt._
import Keys._
import root.base._

object Sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.sampling")

  lazy val combine = OsgiProject("combine") dependsOn (Misc.exception, Domain.modifier, Core.workflow, Tool.groovy)

  lazy val csv = OsgiProject("csv") dependsOn (Misc.exception, Core.workflow, Tool.csv) settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val lhs = OsgiProject("lhs") dependsOn (Misc.exception, Core.workflow, Misc.workspace)

  lazy val quasirandom = OsgiProject("quasirandom") dependsOn (Misc.exception, Core.workflow, Misc.workspace) settings (
    libraryDependencies += Apache.math
  )
}
