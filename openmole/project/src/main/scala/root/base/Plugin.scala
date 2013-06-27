package root.base

import sbt._
import root._
import base.plugin._

abstract class PluginDefaults(subBuilds: Defaults*) extends BaseDefaults(subBuilds: _*) {
  override val dir = file("core/plugins")
}

object Plugin extends PluginDefaults(plugin.Task, Tools, Domain, Builder, Method, Environment, Sampling, Grouping, Hook, Profiler, Source) {}

