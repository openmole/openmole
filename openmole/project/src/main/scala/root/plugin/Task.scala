package plugin

import root.Libraries
import sbt._

import root.Libraries._
import sbt.Keys._
import scala.Some
import root._

//The task plugins for openmole go in here.

//TODO: Consider making the package hierarchy for this match it's artifactID.
object Task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.task")

  lazy val external = OsgiProject("external", imports = Seq("*")) dependsOn (Core.exception, Core.dsl, Core.workspace)

  lazy val netLogo = OsgiProject("netlogo", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, Core.workspace, external, Tool.netLogoAPI)

  //the imports disambiguates netlogo5 from netlogo4
  lazy val netLogo4 = OsgiProject("netlogo4",
    imports = Seq("org.nlogo.*;version=\"[4,5)\"", "*;resolution:=optional")) dependsOn (
      netLogo, Core.workflow, external, Tool.netLogo4API)

  lazy val netLogo5 = OsgiProject("netlogo5") dependsOn (netLogo, Core.workflow, external, Tool.netLogo5API)

  lazy val jvm = OsgiProject("jvm", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, external, Core.workspace)

  lazy val scala = OsgiProject("scala", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, jvm, Core.console)

  lazy val groovy = OsgiProject("groovy", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, Tool.groovy, jvm, Core.replication % "test") settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val template = OsgiProject("template", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, Core.workspace, Core.replication % "test") settings (
    libraryDependencies += Libraries.scalatest
  )

  lazy val systemexec = OsgiProject("systemexec", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, external,
    Core.workspace) settings (
      libraryDependencies ++= Seq(exec)
    )

  lazy val statistic = OsgiProject("statistic", imports = Seq("*")) dependsOn (Core.workflow)

  lazy val tools = OsgiProject("tools", imports = Seq("*")) dependsOn (Core.workflow)

}
