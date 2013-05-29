package root

import root.base._
import sbt._

object Base extends BaseDefaults {
  override def dir = file("core")

  lazy val all = Project("base", dir) aggregate (Core.all, Misc.all, Plugin.all, base.Runtime.all)
}