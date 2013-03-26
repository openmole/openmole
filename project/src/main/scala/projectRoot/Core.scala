package projectRoot

import sbt._

import core.{CorePlugin, CoreMisc, CoreCore}

trait Core extends CoreCore with CoreMisc with CorePlugin {
  lazy val core = Project("core", file("core")) aggregate (coreMisc, coreCore, corePlugin)
}