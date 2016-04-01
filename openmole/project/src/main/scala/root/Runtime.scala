package root

import org.openmole.buildsystem.OMKeys._
import root.Libraries._
import sbt.Keys._
import sbt._

object Runtime extends Defaults(runtime.REST) {
  override def dir = file("runtime")

  lazy val console = OsgiProject("org.openmole.runtime.console", imports = Seq("*")) settings (
    libraryDependencies += upickle
  ) dependsOn (
      Core.workflow,
      Core.console,
      Core.project,
      Core.dsl,
      Core.batch,
      Core.buildinfo
    )

  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set())

}
