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

  lazy val external = OsgiProject("external") dependsOn (provided(misc.exception), core.implementation,
    provided(misc.workspace))

  lazy val netLogo = OsgiProject("netlogo") dependsOn (misc.exception, provided(core.implementation), provided(misc.workspace), provided(external))

  //the imports disambiguates netlogo5 from netlogo4
  lazy val netLogo4 = OsgiProject("netlogo4",
    imports = Seq("org.nlogo.*;version=\"[4,5)\"", "*;resolution:=optional")) dependsOn (provided(netLogo), provided(netlogo4_noscala),
      provided(core.implementation), external)

  lazy val netLogo5 = OsgiProject("netlogo5") dependsOn (provided(netLogo), provided(netlogo5_noscala), provided(core.implementation), external)

  lazy val code = OsgiProject("code") dependsOn (provided(misc.exception), provided(core.implementation), external, provided(misc.workspace))

  lazy val scala = OsgiProject("scala") dependsOn (provided(misc.exception), provided(core.model), provided(code), provided(misc.osgi), provided(scalaLang))

  lazy val groovy = OsgiProject("groovy") dependsOn (provided(misc.exception), provided(core.model), tools.groovy, code)

  lazy val template = OsgiProject("template") dependsOn (provided(misc.exception), provided(core.implementation), provided(misc.workspace))

  lazy val systemexec = OsgiProject("systemexec") dependsOn (provided(misc.exception), provided(core.implementation), external,
    provided(misc.workspace), apache.exec % "provided")

  lazy val stat = OsgiProject("stat") dependsOn (provided(core.implementation))
}