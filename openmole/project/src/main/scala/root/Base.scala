package root

import root.base._
import sbt._
import org.openmole.buildsystem.OMKeys._

object Base extends BaseDefaults(Core, Misc, Plugin, base.Runtime) {
  override def dir = file("core")

  lazy val all = Project("base", dir) aggregate (subProjects: _*) //TODO: Quick hack to workaround the file hungriness of SBT 0.13.0 fix when https://github.com/sbt/sbt/issues/937 is fixed
}

abstract class BaseDefaults(subBuilds: Defaults*) extends Defaults(subBuilds: _*) {
  override val org = "org.openmole.core"

  def dir = file("core") //TODO change to base

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core"))
}