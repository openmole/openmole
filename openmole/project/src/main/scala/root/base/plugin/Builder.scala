package root.base.plugin

import sbt._
import root.base._

object Builder extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.builder")

  lazy val base = OsgiProject("base") dependsOn (Core.implementation)

  lazy val evolution = OsgiProject("evolution") dependsOn (Misc.exception, Core.implementation, Method.evolution, Task.tools)

  lazy val sensitivity = OsgiProject("sensitivity") dependsOn (Core.implementation, plugin.Task.stat, Method.sensitivity)

  lazy val stochastic = OsgiProject("stochastic") dependsOn (Core.implementation, plugin.Task.stat, Domain.distribution)
}
