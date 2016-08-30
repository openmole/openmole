package plugin

import root.Libraries._
import root.{ Libraries, _ }
import sbt.Keys._
import sbt._

//The task plugins for openmole go in here.

//TODO: Consider making the package hierarchy for this match its artifactID.
object Task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.task")

  lazy val external = OsgiProject("external", imports = Seq("*")) dependsOn (Core.dsl, Core.workspace)

  lazy val netLogo = OsgiProject("netlogo", imports = Seq("*")) dependsOn (Core.dsl, external, Tool.netLogoAPI)

  lazy val netLogo5 = OsgiProject("netlogo5") dependsOn (netLogo, Core.dsl, external, Tool.netLogo5API)

  lazy val jvm = OsgiProject("jvm", imports = Seq("*")) dependsOn (Core.dsl, external, Core.workspace)

  lazy val scala = OsgiProject("scala", imports = Seq("*")) dependsOn (Core.dsl, jvm, Core.console)

  lazy val template = OsgiProject("template", imports = Seq("*")) dependsOn (Core.dsl, Core.replication % "test") settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val systemexec = OsgiProject("systemexec", imports = Seq("*")) dependsOn (Core.dsl, external,
    Core.workspace) settings (
      libraryDependencies ++= Seq(exec)
    )

  lazy val care = OsgiProject("care", imports = Seq("*")) dependsOn (systemexec) settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val tools = OsgiProject("tools", imports = Seq("*")) dependsOn (Core.dsl)

}
