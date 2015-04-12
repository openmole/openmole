package root

import com.typesafe.sbt.osgi.OsgiKeys._
import org.openmole.buildsystem.OMKeys._
import root._
import sbt.Keys._
import sbt._

abstract class PluginDefaults(subBuilds: Defaults*) extends Defaults(subBuilds: _*) {
  override val dir = file("plugins")

  lazy val defaultActivator = bundleActivator <<= (name) { n ⇒ Some(n + ".Activator") }

  override def osgiSettings = super.osgiSettings ++
    Seq(
      bundleType := Set("plugin"),
      defaultActivator,
      libraryDependencies += Libraries.equinoxOSGi
    )

}

object Plugin extends PluginDefaults(plugin.Task, plugin.Tool, plugin.Domain, plugin.Method, plugin.Environment, plugin.Sampling, plugin.Grouping, plugin.Hook, plugin.Source) {

}

