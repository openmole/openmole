package root.base.plugin

import sbt._

import root.libraries.Apache
import root.base._
import root.Libraries._

//The task plugins for openmole go in here.

//TODO: Consider making the package hierarchy for this match it's artifactID.
object Task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.task")

  lazy val all = Project("core-tasks", dir) aggregate (external, netLogo, netLogo4, netLogo5, scala, groovy, code,
    systemexec, stat, template)

  lazy val external = OsgiProject("external") dependsOn (provided(Misc.exception), Core.implementation,
    provided(Misc.workspace))

  lazy val netLogo = OsgiProject("netlogo") dependsOn (Misc.exception, provided(Core.implementation), provided(Misc.workspace), provided(external))

  //the imports disambiguates netlogo5 from netlogo4
  lazy val netLogo4 = OsgiProject("netlogo4",
    imports = Seq("org.nlogo.*;version=\"[4,5)\"", "*;resolution:=optional")) dependsOn (provided(netLogo), provided(netlogo4_noscala),
      provided(Core.implementation), external)

  lazy val netLogo5 = OsgiProject("netlogo5") dependsOn (provided(netLogo), provided(netlogo5_noscala), provided(Core.implementation), external)

  lazy val code = OsgiProject("code") dependsOn (provided(Misc.exception), provided(Core.implementation), external, provided(Misc.workspace))

  lazy val scala = OsgiProject("scala") dependsOn (provided(Misc.exception), provided(Core.model), provided(code), provided(Misc.osgi), provided(scalaLang))

  lazy val groovy = OsgiProject("groovy") dependsOn (provided(Misc.exception), provided(Core.model), Tools.groovy, code)

  lazy val template = OsgiProject("template") dependsOn (provided(Misc.exception), provided(Core.implementation), provided(Misc.workspace))

  lazy val systemexec = OsgiProject("systemexec") dependsOn (provided(Misc.exception), provided(Core.implementation), external,
    provided(Misc.workspace), Apache.exec % "provided")

  lazy val stat = OsgiProject("stat") dependsOn (provided(Core.implementation))
}