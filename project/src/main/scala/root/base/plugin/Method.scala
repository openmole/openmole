package root.base.plugin

import root.base._
import root.libraries._
import sbt._

package object method extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.plugin.method")

  lazy val all = Project("core-plugin-method", dir) aggregate (evolution, sensitivity)

  lazy val evolution = OsgiProject("evolution") dependsOn (misc.exception, core.implementation, mgo, misc.workspace) //todo: other plugins have a dependency on MGO

  lazy val sensitivity = OsgiProject("sensitivity") dependsOn (misc.exception, core.implementation)
}