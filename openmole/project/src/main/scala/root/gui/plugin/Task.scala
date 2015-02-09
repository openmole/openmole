package root.gui.plugin

import root.base
import sbt._
import root.gui._
import Keys._
import root._

import com.typesafe.sbt.osgi.OsgiKeys._

object Task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.task")

  val rootDir = dir / artifactPrefix.get

  //FIXME: should be constructed with: lazy val groovy = project("groovy", serverProjectDependencies = Seq(base.plugin.Task.groovy))
  val (ext, client, server) = Util.project(rootDir, "groovy", serverProjectDependencies = Seq(base.plugin.Task.groovy))
  lazy val groovy = OsgiProject("groovy") dependsOn (ext, client, server)

  val (ext1, client1, server1) = Util.project(rootDir, "systemexec", serverProjectDependencies = Seq(base.plugin.Task.systemexec))
  lazy val systemexec = OsgiProject("systemexec") dependsOn (ext1, client1, server1)
}
