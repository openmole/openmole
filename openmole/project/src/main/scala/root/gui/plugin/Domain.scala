package root.gui.plugin

import sbt._
import root.gui._

object Domain extends GUIPluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.domain")
  /*
  lazy val collection = OsgiProject("collection") dependsOn (base.plugin.Domain.collection, base.Misc.replication % "test")

  lazy val distribution = OsgiProject("distribution") dependsOn (base.plugin.Domain.distribution,
    base.plugin.Domain.modifier, base.Misc.replication % "test")

  lazy val file = OsgiProject("file") dependsOn (Server.server, base.plugin.Domain.file, base.Misc.replication % "test")

  lazy val modifier = OsgiProject("modifier") dependsOn ( base.plugin.Domain.modifier, base.Misc.replication % "test")

  lazy val range = OsgiProject("range") dependsOn (base.plugin.Domain.range, base.Misc.replication % "test")*/
}
