package root.gui.plugin

import sbt._
import root.gui._
import com.typesafe.sbt.osgi.OsgiKeys

object Miscellaneous extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.misc")

  lazy val tools = OsgiProject("tools") dependsOn (Core.implementation) settings (OsgiKeys.bundleActivator := None)
}