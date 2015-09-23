package root.gui.plugin

import sbt._
import root.gui._
import root._

import root.Libraries
import Keys._
import org.openmole.buildsystem.OMKeys._

object Environment extends GUIPluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.environment")

  val rootDir = dir / artifactPrefix.get

  //FIXME: should be constructed with: lazy val groovy = project("groovy", serverProjectDependencies = Seq(base.plugin.Task.groovy))
  val (ext, client, server) = Util.project(rootDir, "ssh",
    serverProjectDependencies = Seq(Core.batch, Core.workspace, _root_.plugin.Environment.ssh))

  lazy val ssh = OsgiProject("ssh") dependsOn (ext, client, server)

  val (ext1, client1, server1) = Util.project(rootDir, "egi",
    serverProjectDependencies = Seq(Core.batch, Core.workspace, _root_.plugin.Environment.egi))

  lazy val egi = OsgiProject("egi") dependsOn (ext1, client1, server1)

}
