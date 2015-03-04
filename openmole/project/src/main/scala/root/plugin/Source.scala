package plugin

import sbt._
import Keys._
import root.Libraries._
import root._

object Source extends root.PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.source")

  lazy val fileSource = OsgiProject("file", imports = Seq("*")) dependsOn (Core.workflow, Core.serializer, Core.exception, Tool.csv)
}

