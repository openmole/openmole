package root.gui.plugin

import root.base
import sbt._
import root.gui._
import Keys._
import root._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
//import ScalaJSKeys._

import com.typesafe.sbt.osgi.OsgiKeys._

object Task extends PluginDefaults {
  implicit val artifactPrefix = Some("org.openmole.gui.plugin.task")

  val rootDir = dir / artifactPrefix.get

  def project(suffix: String,
              extProjectDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(),
              extLibDependencies: Seq[ModuleID] = Seq(),
              clientProjectDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(),
              clientLibDependencies: Seq[ModuleID] = Seq(),
              serverProjectDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(),
              serverLibDependencies: Seq[ModuleID] = Seq()) = {
    lazy val ext = subProject(suffix + ".ext", extProjectDependencies, extLibDependencies ++ Seq(root.Libraries.scalaTagsJS)) dependsOn (Ext.data) settings (scalaJSSettings: _*)
    lazy val client = subProject(suffix + ".client", clientProjectDependencies, clientLibDependencies) dependsOn (ext, Ext.dataui, Ext.factoryui, Bootstrap.osgi, base.Misc.replication % "test") settings (scalaJSSettings: _*)
    lazy val server = subProject(suffix + ".server", serverProjectDependencies, serverLibDependencies) dependsOn (ext, Server.factory, base.Misc.replication % "test")

    //FIXME: how to call directly OsgiProject from here ?
    // OsgiProject(suffix) dependsOn (ext, client, server)

    (ext, client, server)
  }

  def subProject(suffix: String,
                 extProjectDependencies: Seq[ClasspathDep[ProjectReference]] = Seq(),
                 extLibDependencies: Seq[ModuleID] = Seq()) =
    Project(suffix.replace('.', '-'), new File(rootDir + "." + suffix)) settings (libraryDependencies ++= extLibDependencies) dependsOn (extProjectDependencies: _*)

  //FIXME: should be constructed with: lazy val groovy = project("groovy", serverProjectDependencies = Seq(base.plugin.Task.groovy))
  val (ext, client, server) = project("groovy", serverProjectDependencies = Seq(base.plugin.Task.groovy))
  lazy val groovy = OsgiProject("groovy") dependsOn (ext, client, server)

  val (ext1, client1, server1) = project("systemexec", serverProjectDependencies = Seq(base.plugin.Task.systemexec))
  lazy val systemexec = OsgiProject("systemexec") dependsOn (ext1, client1, server1)

  //
  // lazy val groovy = project("groovy", serverProjectDependencies = Seq(base.plugin.Task.groovy))

  //lazy val groovyExt = project("groovy.ext") dependsOn (Ext.data)
  // lazy val groovyClient = project("groovy.client") dependsOn(groovyExt, Client.dataui, base.Misc.replication % "test")
  //lazy val groovyServer = project("groovy.server") dependsOn(groovyExt, Server.factory, base.plugin.Task.groovy, base.Misc.replication % "test")

  // lazy val groovy = OsgiProject("groovy") dependsOn (groovyServer) settings (bundle <<= bundle dependsOn (
  //   toJar in groovyExt, toJar in groovyClient)) settings (jsManagerSettings: _*) settings (scalaJSSettings: _*)

  // lazy val groovy = project("groovy") dependsOn (groovyServer, groovyExt, groovyClient) settings (bundle <<= bundle dependsOn (
  //   toJar in target)) settings (jsManagerSettings: _*) settings (scalaJSSettings: _*)

  /* lazy val groovyTmp = project("groovy.tmp") dependsOn (groovyServer, groovyExt, groovyClient) settings (packageSrc <<= packageSrc dependsOn (
    toJar in target)) settings (jsManagerSettings: _*) settings (scalaJSSettings: _*)*/

  //lazy val groovy = OsgiProject("groovy") dependsOn(groovyServer, groovyExt, groovyClient)

  // fullOptJS in groovyExt in Compile, fullOptJS in groovyClient in Compile)) settings (jsManagerSettings: _*) settings (scalaJSSettings: _*)

  /*settings (bundle <<= bundle dependsOn (
    fullOptJS in groovyExt in Compile, fullOptJS in groovyClient in Compile))*/

  //  settings(bundle <<= bundle dependsOn (fullOptJS in groovyExt)) //, groovyClient, groovyServer)

  /* lazy val template = OsgiProject("template") dependsOn (Ext.data, base.Misc.workspace, base.plugin.Task.template, base.Misc.replication % "test")

  lazy val moletask = OsgiProject("moletask") dependsOn (Ext.data, base.Core.model, base.Misc.replication % "test")

  lazy val netlogo = OsgiProject("netlogo") dependsOn (Ext.data, base.Core.model,
    provided(base.plugin.Task.netLogo4), provided(base.plugin.Task.netLogo5), base.Misc.replication % "test", base.plugin.Tool.netLogo4API, base.plugin.Tool.netLogo5API)


  lazy val systemexec = OsgiProject("systemexec") dependsOn (Ext.data, base.plugin.Task.systemexec,
    base.Core.model)*/

}
