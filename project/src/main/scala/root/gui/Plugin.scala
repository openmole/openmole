package root.gui

import root.base
import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys._

trait PluginDefaults extends GuiDefaults {
  override def dir = super.dir / "plugins"

  override lazy val OsgiSettings = super.OsgiSettings ++ Seq(bundleActivator <<= (name) { n ⇒ Some(n + ".Activator") })
}

package object plugin extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin")

  lazy val all = Project("gui-plugin", dir) aggregate (task.all, domain.all, environment.all, sampling.all, builder.all,
    groupingstrategy, miscellaneous.all, hook.all, method.all, source.all)

  lazy val groupingstrategy = OsgiProject("groupingstrategy") dependsOn (core.implementation, misc.widget,
    base.plugin.grouping.batch, base.core.implementation)
}