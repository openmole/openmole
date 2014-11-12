package root.base.plugin

import root.libraries._
import sbt._
import Keys._
import root.base._
import root.Libraries._

object Sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.sampling")

  lazy val combine = OsgiProject("combine") dependsOn (Misc.exception, Domain.modifier, Core.implementation, Tool.groovy)

  lazy val csv = OsgiProject("csv") dependsOn (Misc.exception, Core.implementation) settings (
    libraryDependencies += opencsv
  )

  lazy val hypothesis = OsgiProject("hypothesis") dependsOn (Misc.exception, Core.implementation)

  lazy val lhs = OsgiProject("lhs") dependsOn (Misc.exception, Core.implementation, Misc.workspace)

  lazy val quasirandom = OsgiProject("quasirandom") dependsOn (Misc.exception, Core.implementation, Misc.workspace) settings (
    libraryDependencies += Apache.math
  )
}
