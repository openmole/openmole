package root.base

import sbt._


trait PluginDefaults extends BaseDefaults {
  val dir = file("core/plugins")

}

package object plugin extends PluginDefaults {
  lazy val all = Project("core-plugin", dir) aggregate (task.all, tools.all)
}

