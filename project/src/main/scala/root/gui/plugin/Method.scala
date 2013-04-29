package root.gui.plugin

import root.base
import sbt._
import root.gui._

package object method extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.ide.plugin.method")

  lazy val all = Project("gui-plugin-method", dir) aggregate (sensitivity)

  lazy val sensitivity = OsgiProject("sensitivity") dependsOn (base.plugin.method.sensitivity, domain.range,
    core.implementation)
}