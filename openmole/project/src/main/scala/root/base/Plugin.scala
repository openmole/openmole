package root.base

import sbt._
import root._

trait PluginDefaults extends BaseDefaults {
  override val dir = file("core/plugins")
}

object Plugin extends PluginDefaults {
  import base.plugin._
  implicit val artifactPrefix = Some("org.openmole.plugin")

  lazy val all = Project("core-plugin", dir) aggregate (Task.all, Tools.all, Domain.all, Builder.all, Method.all,
    Environment.all, Sampling.all, Grouping.all, Hook.all, Profiler.all, Source.all) //TODO: name inconsistency
}

