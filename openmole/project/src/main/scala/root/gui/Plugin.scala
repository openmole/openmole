package root.gui

import root._
import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys._
import root.gui.plugin._

abstract class PluginDefaults(subBuilds: Defaults*) extends GuiDefaults(subBuilds: _*) {
  override def dir = super.dir / "plugins"

  override lazy val OsgiSettings = super.OsgiSettings ++ Seq(bundleActivator <<= (name) { n ⇒ Some(n + ".Activator") })
}

object Plugin extends PluginDefaults(plugin.Task, Domain, Environment, Sampling, Builder, Miscellaneous, Hook, Method, Source) {

  implicit val artifactPrefix = Some("org.openmole.ide.plugin")

  lazy val groupingstrategy = OsgiProject("groupingstrategy") dependsOn (Core.implementation, base.plugin.Grouping.batch,
    base.Core.model)
}