package root

import root.base._
import sbt._
import org.openmole.buildsystem.OMKeys._

object Base extends BaseDefaults(Core, Misc, Plugin, base.Runtime) {
  override def dir = file("core")
}

abstract class BaseDefaults(subBuilds: Defaults*) extends Defaults(subBuilds: _*) {
  override val org = "org.openmole.core"

  def dir = file("core") //TODO change to base

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core"))
}