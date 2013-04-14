package root.base.plugin

import sbt._

import root.libraries._
import root.base._

//The task plugins for openmole go in here.

//TODO: Consider making the package hierarchy for this match it's artifactID.
package object task extends PluginDefaults {
  implicit val artifactPrefix = "org.openmole.plugin.task"

  lazy val all = Project("core-tasks", dir) aggregate (external, netLogo, netLogo4, netLogo5)

  lazy val external = OsgiProject("org.openmole.plugin.task.external") dependsOn (misc.exception, core.implementation,
    misc.workspace)

  lazy val netLogo = OsgiProject("org.openmole.plugin.task.netlogo") dependsOn (core.implementation, misc.exception,
    misc.workspace, external)

  //the imports disambiguates netlogo5 from netlogo4
  lazy val netLogo4 = OsgiProject("org.openmole.plugin.task.netlogo4",
    imports=Seq("org.nlogo.*;version=\"[4,5)\"", "*;resolution:=optional")) dependsOn (netLogo, netlogo4_noscala)

  lazy val netLogo5 = OsgiProject("org.openmole.plugin.task.netlogo5") dependsOn (netLogo, netlogo5_noscala)

  lazy val code = OsgiProject("org.openmole.plugin.task.code") dependsOn (misc.exception, external, core.implementation,
    misc.workspace)

  lazy val scala = OsgiProject("scala") dependsOn (misc.exception, core.implementation, code, misc.osgi)


  lazy val groovy = OsgiProject("groovy") dependsOn (misc.exception, core.implementation, tools.groovy, code)



}