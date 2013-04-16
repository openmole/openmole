package root.base

import sbt._

trait PluginDefaults extends BaseDefaults {
  val dir = file("core/plugins")
}

package object plugin extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin")

  lazy val all = Project("core-plugin", dir) aggregate (task.all, tools.all, domain.all, builder.all, method.all)

  // TODO - is this needed?
  // lazy val builder = OsgiProject("builder") dependsOn (misc.exception, core.implementation)
}

