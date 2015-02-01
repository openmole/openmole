package root.base.plugin

import root.Libraries
import sbt._

import root.libraries.Apache
import root.base._
import root.Libraries._
import sbt.Keys._
import scala.Some

//The task plugins for openmole go in here.

//TODO: Consider making the package hierarchy for this match it's artifactID.
object Task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.task")

  lazy val external = OsgiProject("external") dependsOn (Misc.exception, Core.dsl, Misc.workspace)

  lazy val netLogo = OsgiProject("netlogo") dependsOn (Misc.exception, Core.workflow, Misc.workspace, external, Tool.netLogoAPI)

  //the imports disambiguates netlogo5 from netlogo4
  lazy val netLogo4 = OsgiProject("netlogo4",
    imports = Seq("org.nlogo.*;version=\"[4,5)\"", "*;resolution:=optional")) dependsOn (
      netLogo, Core.workflow, external, Tool.netLogo4API)

  lazy val netLogo5 = OsgiProject("netlogo5") dependsOn (netLogo, Core.workflow, external, Tool.netLogo5API)

  lazy val jvm = OsgiProject("jvm") dependsOn (Misc.exception, Core.workflow, external, Misc.workspace)

  lazy val scala = OsgiProject("scala") dependsOn (Misc.exception, Core.workflow, jvm, Misc.console)

  lazy val groovy = OsgiProject("groovy") dependsOn (Misc.exception, Core.workflow, Tool.groovy, jvm, Misc.replication % "test")

  lazy val template = OsgiProject("template") dependsOn (Misc.exception, Core.workflow, Misc.workspace, Misc.replication % "test")

  lazy val systemexec = OsgiProject("systemexec") dependsOn (Misc.exception, Core.workflow, external,
    Misc.workspace) settings (
      libraryDependencies ++= Seq(Apache.exec)
    )

  lazy val statistic = OsgiProject("statistic") dependsOn (Core.workflow)

  lazy val tools = OsgiProject("tools") dependsOn (Core.workflow)

}
