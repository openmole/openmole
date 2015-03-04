package root.gui.plugin

import sbt._
import root.gui._
import Keys._

import com.typesafe.sbt.osgi.OsgiKeys._

object Task extends GUIPluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.task")

  val rootDir = dir / artifactPrefix.get

  //FIXME: should be constructed with: lazy val groovy = project("groovy", serverProjectDependencies = Seq(base.plugin.Task.groovy))
  val (ext, client, server) = Util.project(rootDir, "groovy", serverProjectDependencies = Seq(_root_.plugin.Task.groovy))
  lazy val groovy = OsgiProject("groovy") dependsOn (ext, client, server)

  val (ext1, client1, server1) = Util.project(rootDir, "systemexec", serverProjectDependencies = Seq(_root_.plugin.Task.systemexec))
  lazy val systemexec = OsgiProject("systemexec") dependsOn (ext1, client1, server1)

  val (ext2, client2, server2) = Util.project(rootDir, "statistic", serverProjectDependencies = Seq(_root_.plugin.Task.statistic))
  lazy val statistic = OsgiProject("statistic") dependsOn (ext2, client2, server2)
}
