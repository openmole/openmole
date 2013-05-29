package root.base.plugin

import sbt._
import root.base._
import root.Libraries._

object Source extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.source")

  lazy val all = Project("base-plugin-source", dir) aggregate (file)

  lazy val file = OsgiProject("file") dependsOn (provided(Core.implementation), provided(opencsv), provided(Core.serializer), provided(Misc.exception))
}

