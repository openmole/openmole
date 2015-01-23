package root.base.plugin

import root.base._
import root.Libraries._
import sbt._
import Keys._

object Method extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.method")

  lazy val evolution = OsgiProject("evolution") dependsOn (Misc.exception, Core.workflow, Misc.workspace, Hook.fileHook, plugin.Task.tools) settings
    (libraryDependencies += mgo) //todo: other plugins have a dependency on MGO

  lazy val stochastic = OsgiProject("stochastic") dependsOn (Core.workflow, plugin.Task.statistic, Domain.distribution)

  lazy val abc = OsgiProject("abc") dependsOn (Misc.exception, Core.workflow, Misc.workspace, Hook.fileHook, plugin.Task.tools) settings
    (libraryDependencies += scalabc)

  lazy val modelFamily = OsgiProject("modelfamily") dependsOn (evolution, Core.workflow, Task.scala)

}
