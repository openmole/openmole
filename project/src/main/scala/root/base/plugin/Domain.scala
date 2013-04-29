package root.base.plugin

import sbt._
import root.base._
import root.libraries._

package object domain extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.domain")

  lazy val all = Project("core-plugin-domain", dir) aggregate (bounded, collection, distribution, file, modifier, range,
    relative)

  lazy val bounded = OsgiProject("bounded") dependsOn (core.implementation)

  lazy val collection = OsgiProject("collection") dependsOn (misc.exception, core.implementation)

  lazy val distribution = OsgiProject("distribution") dependsOn (misc.exception, core.implementation, apache.math % "provided")

  lazy val file = OsgiProject("file") dependsOn (misc.exception, core.implementation)

  lazy val modifier = OsgiProject("modifier") dependsOn (misc.exception, core.implementation, tools.groovy)

  lazy val range = OsgiProject("range") dependsOn (core.implementation, misc.exception)

  lazy val relative = OsgiProject("relative") dependsOn (misc.exception, core.implementation)
}