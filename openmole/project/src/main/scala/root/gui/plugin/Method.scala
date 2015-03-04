package root.gui.plugin

import sbt._
import root.gui._

object Method extends GUIPluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.method")
  /*
  lazy val sensitivity = OsgiProject("sensitivity") dependsOn (base.plugin.Method.sensitivity, Domain.range,
    Ext.data, base.Misc.replication % "test")*/
}
