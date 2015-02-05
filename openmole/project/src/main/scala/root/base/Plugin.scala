package root.base

import sbt._
import root._
import base.plugin._
import org.openmole.buildsystem.OMKeys._

abstract class PluginDefaults(subBuilds: Defaults*) extends BaseDefaults(subBuilds: _*) {
  override val dir = file("core/plugins")
  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set("plugin"))

}

object Plugin extends PluginDefaults(plugin.Task, Tool, Domain, Method, Environment, Sampling, Grouping, Hook, Source) {
}

