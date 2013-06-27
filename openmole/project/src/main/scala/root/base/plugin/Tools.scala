package root.base.plugin

import root.base._
import root.Libraries
import sbt._

object Tools extends PluginDefaults {

  implicit val artifactPrefix = Some("org.openmole.plugin.tools")

  lazy val groovy = OsgiProject("groovy") dependsOn (provided(Misc.exception), provided(Core.implementation),
    provided(Libraries.groovy))
}