package root.base

import root.Defaults
import sbt._
import Keys._

trait BaseDefaults extends Defaults {
  override lazy val org = organization := "org.openmole.core"
}