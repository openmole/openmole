package plugin

import root.Libraries._
import sbt._
import Keys._
import root._

object Method extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.method")

  lazy val evolution = OsgiProject("evolution", imports = Seq("*")) dependsOn (
    Core.dsl, Hook.fileHook, Task.tools, Tool.pattern
  ) settings (libraryDependencies += mgo)

  lazy val stochastic = OsgiProject("stochastic", imports = Seq("*")) dependsOn (Core.dsl, Domain.distribution, Tool.pattern)

  lazy val abc = OsgiProject("abc", imports = Seq("*")) dependsOn (Core.dsl, Hook.fileHook, Task.tools) settings
    (libraryDependencies += scalabc)

  /*lazy val modelFamily = OsgiProject("modelfamily", imports = Seq("*")) dependsOn (evolution, Core.workflow, Task.scala) settings (
    libraryDependencies += family
  )*/

}
