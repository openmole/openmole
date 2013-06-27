package root

import root.base._
import sbt._

object Base extends BaseDefaults(Core, Misc, Plugin, base.Runtime) {
  override def dir = file("core")

  /*lazy val all = Project("base", dir) aggregate (Core.all, Misc.all, Plugin.all, base.Runtime.all)*/

  override def subProjects = Core.projectRefs ++ Misc.projectRefs ++ Plugin.projectRefs ++ base.Runtime.projectRefs
}

abstract class BaseDefaults(subBuilds: Defaults*) extends Defaults(subBuilds: _*) {
  override val org = "org.openmole.core"

  def dir = file("core") //TODO change to base
}