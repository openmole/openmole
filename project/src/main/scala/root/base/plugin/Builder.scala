package root.base.plugin

import sbt._
import root.base._

package object builder extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.builder")

  lazy val all = Project("core-plugin-builder", dir) aggregate (base, evolution, sensitivity, stochastic)

  lazy val base = OsgiProject("base") dependsOn (core.implementation)

  lazy val evolution = OsgiProject("evolution") dependsOn (misc.exception, core.implementation, method.evolution)

  lazy val sensitivity = OsgiProject("sensitivity") dependsOn (core.implementation, task.stat, method.sensitivity)

  lazy val stochastic = OsgiProject("stochastic") dependsOn (core.implementation, task.stat, domain.distribution)
}