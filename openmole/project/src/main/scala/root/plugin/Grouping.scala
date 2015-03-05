package plugin

import sbt._
import root._

object Grouping extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.grouping")

  lazy val batch = OsgiProject("batch", imports = Seq("*")) dependsOn (Core.exception, Core.workflow, Core.workspace)

  lazy val onvariable = OsgiProject("onvariable", imports = Seq("*")) dependsOn (Core.exception, Core.workflow)
}