package root.base.plugin

import sbt._
import root.base._
import root.libraries._

package object source extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.source")

  lazy val all = Project("base-plugin-source", dir) aggregate (file)

  lazy val file = OsgiProject("file") dependsOn (provided(core.implementation), provided(opencsv), provided(core.serializer), provided(misc.exception))
}

