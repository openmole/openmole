package root

import sbt._

package object base extends Defaults {
  lazy val dir = file("core")
  lazy val all = Project("core", dir) aggregate (misc.all, core.all, plugin.all)
}