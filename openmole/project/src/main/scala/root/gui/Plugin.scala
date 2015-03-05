package root.gui

import root._
import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys._
import root.gui.plugin._
import org.openmole.buildsystem.OMKeys._

abstract class GUIPluginDefaults(subBuilds: Defaults*) extends GuiDefaults(subBuilds: _*) {
  override def dir = super.dir / "plugins"

  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set("guiPlugin"),
    bundleActivator <<= (name) { n ⇒ Some(n + ".Activator") },
    libraryDependencies ++= Seq(root.Libraries.rx, root.Libraries.scalaTags, root.Libraries.scalajsDom, root.Libraries.scaladget))
}

object Plugin extends GUIPluginDefaults(plugin.Task, Domain, Environment, Sampling, Hook, Method, Source) {

  implicit val artifactPrefix = Some("org.openmole.gui.plugin")

  /* lazy val groupingstrategy = OsgiProject("groupingstrategy") dependsOn (base.plugin.Grouping.batch,
    base.Core.model)*/

}
