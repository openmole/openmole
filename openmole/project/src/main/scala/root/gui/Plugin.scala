package root.gui

import root.base
import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys._

trait PluginDefaults extends GuiDefaults {
  override def dir = super.dir / "plugins"

  override lazy val OsgiSettings = super.OsgiSettings ++ Seq(bundleActivator <<= (name) { n ⇒ Some(n + ".Activator") })
}

object Plugin extends PluginDefaults {
  import root.gui.plugin._

  implicit val artifactPrefix = Some("org.openmole.ide.plugin")

  lazy val all = Project("gui-plugin", dir) aggregate (Task.all, Domain.all, Environment.all, Sampling.all, Builder.all,
    groupingstrategy, Miscellaneous.all, Hook.all, Method.all, Source.all)

  lazy val groupingstrategy = OsgiProject("groupingstrategy") dependsOn (Core.implementation, base.plugin.Grouping.batch,
    base.Core.model)
}