package root.base.plugin

import sbt._

import root.libraries._
import root.base._
import root.base

//The task plugins for openmole go in here.

//TODO: Consider making the package hierarchy for this match it's artifactID.
package object task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.task")

  lazy val all = Project("core-tasks", dir) aggregate (external, netLogo, netLogo4, netLogo5, scala, groovy, code,
    systemexec, stat, template)

  lazy val external = OsgiProject("external") dependsOn (misc.exception, core.implementation,
    misc.workspace)

  lazy val netLogo = OsgiProject("netlogo") dependsOn (core.implementation, misc.exception,
    misc.workspace, external)

  //the imports disambiguates netlogo5 from netlogo4
  lazy val netLogo4 = OsgiProject("netlogo4",
    imports=Seq("org.nlogo.*;version=\"[4,5)\"", "*;resolution:=optional")) dependsOn (netLogo, netlogo4_noscala)

  lazy val netLogo5 = OsgiProject("netlogo5") dependsOn (netLogo, netlogo5_noscala)

  lazy val code = OsgiProject("code") dependsOn (misc.exception, external, core.implementation,
    misc.workspace)

  lazy val scala = OsgiProject("scala") dependsOn (misc.exception, core.implementation, code, misc.osgi)


  lazy val groovy = OsgiProject("groovy") dependsOn (misc.exception, core.implementation, base.plugin.tools.groovy, code)

  lazy val template = OsgiProject("template") dependsOn (misc.exception, core.implementation, misc.workspace)

  lazy val systemexec = OsgiProject("systemexec") dependsOn (misc.exception, core.implementation, external,
    misc.workspace, apache.exec)

  lazy val stat = OsgiProject("stat") dependsOn(core.implementation)
}