package root.base.plugin

import sbt._
import Keys._
import root.base._
import root.Libraries._

object Sampling extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.sampling")

  lazy val combine = OsgiProject("combine") dependsOn (provided(Misc.exception), provided(Domain.modifier), provided(Core.implementation), provided(Tool.groovy))

  lazy val csv = OsgiProject("csv") dependsOn (provided(Misc.exception), provided(Core.implementation)) settings (
    libraryDependencies += opencsv % "provided"
  )

  lazy val hypothesis = OsgiProject("hypothesis") dependsOn (provided(Misc.exception), provided(Core.implementation))

  lazy val lhs = OsgiProject("lhs") dependsOn (provided(Misc.exception), provided(Core.implementation), provided(Misc.workspace))
}
