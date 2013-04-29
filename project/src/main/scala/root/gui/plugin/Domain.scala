package root.gui.plugin

import root.base
import sbt._
import root.gui._

package object domain extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.domain")

  lazy val all = Project("gui-plugin-domain", dir) aggregate (collection, distribution, file, modifier, range)

  lazy val collection = OsgiProject("collection") dependsOn (core.implementation, base.plugin.domain.collection)

  lazy val distribution = OsgiProject("distribution") dependsOn (core.implementation, base.plugin.domain.distribution,
    base.plugin.domain.modifier)

  lazy val file = OsgiProject("file") dependsOn (core.implementation, base.plugin.domain.file)

  lazy val modifier = OsgiProject("modifier") dependsOn (core.implementation, base.plugin.domain.modifier)

  lazy val range = OsgiProject("range") dependsOn (core.implementation, base.plugin.domain.range, base.plugin.domain.bounded)
}