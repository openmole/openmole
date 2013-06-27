package root.gui.plugin

import root.base
import sbt._
import root.gui._

object Domain extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.domain")

  lazy val collection = OsgiProject("collection") dependsOn (Core.implementation, base.plugin.Domain.collection)

  lazy val distribution = OsgiProject("distribution") dependsOn (Core.implementation, base.plugin.Domain.distribution,
    base.plugin.Domain.modifier)

  lazy val file = OsgiProject("file") dependsOn (Core.implementation, base.plugin.Domain.file)

  lazy val modifier = OsgiProject("modifier") dependsOn (Core.implementation, base.plugin.Domain.modifier)

  lazy val range = OsgiProject("range") dependsOn (Core.implementation, base.plugin.Domain.range, base.plugin.Domain.bounded)
}