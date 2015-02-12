package root.base

import sbt._
import root._
import base.plugin._
import org.openmole.buildsystem.OMKeys._
import com.typesafe.sbt.osgi.OsgiKeys._
import Keys._

abstract class PluginDefaults(subBuilds: Defaults*) extends BaseDefaults(subBuilds: _*) {
  override val dir = file("core/plugins")
  override def osgiSettings = super.osgiSettings ++
    Seq(
      bundleType := Set("plugin"),
      bundleActivator <<= (name) { n ⇒ Some(n + ".Activator") },
      libraryDependencies += Libraries.equinoxOSGi
    )

}

object Plugin extends PluginDefaults(plugin.Task, Tool, Domain, Method, Environment, Sampling, Grouping, Hook, Source) {

}

