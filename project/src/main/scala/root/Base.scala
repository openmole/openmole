package root

import sbt._

package object base extends BaseDefaults {
  lazy val dir = file("core")

  lazy val all = Project("base", dir) aggregate (core.all, misc.all, plugin.all)
}