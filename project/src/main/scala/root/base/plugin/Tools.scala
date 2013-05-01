package root.base.plugin

import root.base._
import root.libraries
import sbt._

package object tools extends PluginDefaults {

  implicit val artifactPrefix = Some("org.openmole.plugin.tools")

  lazy val all = Project("core-plugin-tools", dir) aggregate (groovy)

  lazy val groovy = OsgiProject("groovy") dependsOn (provided(misc.exception), provided(core.implementation),
    provided(libraries.groovy))
}