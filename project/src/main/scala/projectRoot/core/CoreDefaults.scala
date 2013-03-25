package projectRoot.core

import projectRoot.Defaults
import sbt._
import Keys._

trait CoreDefaults extends Defaults {
  private[core] implicit val org = organization := "org.openmole.core"
}