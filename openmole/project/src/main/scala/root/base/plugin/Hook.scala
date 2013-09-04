package root.base.plugin

import sbt._
import root.base._
import root.Libraries._

object Hook extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.hook")

  lazy val display = OsgiProject("display") dependsOn (Misc.exception, Core.implementation, Misc.workspace)

  lazy val file = OsgiProject("file") dependsOn (Misc.exception, Core.implementation, Misc.workspace, Core.serializer, Misc.hashService % "test",
    Misc.replication % "test", gnuCrypto % "test")
}