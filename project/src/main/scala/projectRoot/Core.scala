package projectRoot

import sbt._

import core.{CoreMisc, CoreCore}

trait Core extends CoreCore with CoreMisc {
  lazy val core = Project("core", file("core")) aggregate (coreMisc, coreCore)
}