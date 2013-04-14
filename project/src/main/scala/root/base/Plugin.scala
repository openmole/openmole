package root.base

import sbt._


trait PluginDefaults extends BaseDefaults {
  val dir = file("core/plugins")

}

package object plugin extends PluginDefaults {
  import root.base.plugin._
  lazy val all = Project("core-plugin", dir) aggregate (task.all, tools.all)
}

