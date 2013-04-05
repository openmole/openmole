package root.base

import sbt._


package object plugin extends BaseDefaults {
  import core._
  import misc._
  import root.libraries._

  implicit val dir = file("core/plugins")

  lazy val all = Project("core-plugin", dir) aggregate (external, netLogo, netLogo4,
    netLogo5)

  lazy val external = OsgiProject("org.openmole.plugin.task.external") dependsOn (exception,
    implementation, workspace)

  lazy val netLogo = OsgiProject("org.openmole.plugin.task.netlogo") dependsOn (implementation,
    exception, workspace, external)

  //the imports disambiguates netlogo5 from netlogo4
  lazy val netLogo4 = OsgiProject("org.openmole.plugin.task.netlogo4",
    imports=Seq("org.nlogo.*;version=\"[4,5)\"", "*;resolution:=optional")) dependsOn (netLogo,
    netlogo4_noscala)

  lazy val netLogo5 = OsgiProject("org.openmole.plugin.task.netlogo5") dependsOn (netLogo,
    netlogo5_noscala)

}
