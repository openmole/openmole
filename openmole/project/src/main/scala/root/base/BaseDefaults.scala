package root.base

import root.Defaults
import sbt._
import Keys._

trait BaseDefaults extends Defaults {
  override val org = "org.openmole.core"

  def dir = file("core") //TODO change to base
}