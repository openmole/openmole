package plugin

import sbt._
import root._
import root.base._

object Grouping extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.grouping")

  lazy val batch = OsgiProject("batch", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow, Misc.workspace)

  lazy val onvariable = OsgiProject("onvariable", imports = Seq("*")) dependsOn (Misc.exception, Core.workflow)
}