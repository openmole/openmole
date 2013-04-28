package root.base

import sbt._
import root._

trait PluginDefaults extends BaseDefaults {
  override val dir = file("core/plugins")
}

package object plugin extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin")

  lazy val all = Project("core-plugin", dir) aggregate (task.all, tools.all, domain.all, builder.all, method.all,
    environment.all, sampling.all, grouping.all, hook.all, profiler.all, source.all)
}

