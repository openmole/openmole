package root.base.plugin

import sbt._
import root.base._

object Builder extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.builder")

  lazy val all = Project("core-plugin-builder", dir) aggregate (base, evolution, sensitivity, stochastic)

  lazy val base = OsgiProject("base") dependsOn (Core.implementation)

  lazy val evolution = OsgiProject("evolution") dependsOn (Misc.exception, Core.implementation, Method.evolution)

  lazy val sensitivity = OsgiProject("sensitivity") dependsOn (Core.implementation, Task.stat, Method.sensitivity)

  lazy val stochastic = OsgiProject("stochastic") dependsOn (Core.implementation, Task.stat, Domain.distribution)
}