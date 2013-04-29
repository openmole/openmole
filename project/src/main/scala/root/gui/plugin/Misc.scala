package root.gui.plugin

import sbt._
import root.gui._
import com.typesafe.sbt.osgi.OsgiKeys

package object miscellaneous extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.misc")

  lazy val all = Project("core-plugin-misc", dir) aggregate (tools)

  lazy val tools = OsgiProject("tools") dependsOn (core.implementation) settings (OsgiKeys.bundleActivator := None)
}