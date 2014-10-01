package root.gui

import root._
import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys._
import root.gui.plugin._
import org.openmole.buildsystem.OMKeys._
import scala.Some

abstract class PluginDefaults(subBuilds: Defaults*) extends GuiDefaults(subBuilds: _*) {
  override def dir = super.dir / "plugins"

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("guiPlugin"),
    bundleActivator <<= (name) { n ⇒ Some(n + ".Activator") },
    libraryDependencies ++= Seq(root.Libraries.scalaRxJS, root.Libraries.scalaTagsJS, root.Libraries.scalajsDom, root.Libraries.scaladget))
}

object Plugin extends PluginDefaults(plugin.Task, Domain, Environment, Sampling, Hook, Method, Source) {

  implicit val artifactPrefix = Some("org.openmole.gui.plugin")

  /* lazy val groupingstrategy = OsgiProject("groupingstrategy") dependsOn (base.plugin.Grouping.batch,
    base.Core.model)*/

}