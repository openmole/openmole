package root

import root.base.Misc
import root.libraries.Apache
import sbt._
import Keys._

import org.openmole.buildsystem.OMKeys._
import org.openmole.buildsystem._, Assembly._
import Libraries._
import com.typesafe.sbt.osgi.OsgiKeys._
import sbt.inc.Analysis
import sbtunidoc.Plugin._
import UnidocKeys._

import scala.util.matching.Regex

object Bin extends Defaults(Base, Gui, Libraries, ThirdParties, Web) {
  val dir = file("bin")

  def filter(m: ModuleID) = {
    m.organization == "org.eclipse.core" ||
      m.organization == "fr.iscpif.gridscale.bundle" ||
      m.organization == "org.bouncycastle" ||
      m.organization.contains("org.openmole")
  }

  lazy val equinox = Seq(
    equinoxApp intransitive (),
    equinoxContenttype intransitive (),
    equinoxJobs intransitive (),
    equinoxRuntime intransitive (),
    equinoxCommon intransitive (),
    equinoxLauncher intransitive (),
    equinoxRegistry intransitive (),
    equinoxPreferences intransitive (),
    equinoxOsgi intransitive ()
  )

  lazy val renameEquinox =
    Map[Regex, String ⇒ String](
      """org\.eclipse\.equinox\.launcher.*\.jar""".r -> { s ⇒ "org.eclipse.equinox.launcher.jar" },
      """org\.eclipse\.(core|equinox|osgi)""".r -> { s ⇒ s.replaceFirst("-", "_") }
    )

  lazy val openmoleui = OsgiProject("org.openmole.ui", singleton = true, buddyPolicy = Some("global")) settings (
    organization := "org.openmole.ui"
  ) settings (
      libraryDependencies ++= Seq(jodaTime, scalaLang, jasypt, Apache.config, Apache.ant, jline, Apache.log4j, scopt, equinoxApp)
    ) dependsOn (
        base.Misc.workspace, base.Misc.replication, base.Misc.exception, base.Misc.tools, base.Misc.eventDispatcher,
        base.Misc.pluginManager, base.Core.implementation, base.Core.batch, gui.Server.core, gui.Client.core, gui.Bootstrap.js, gui.Bootstrap.osgi, base.Misc.sftpserver, base.Misc.logging,
        Web.core, base.Misc.console)

  lazy val java368URL = new URL("http://maven.iscpif.fr/thirdparty/com/oracle/java-jre-linux-386/20-b17/java-jre-linux-386-20-b17.tgz")
  lazy val javax64URL = new URL("http://maven.iscpif.fr/thirdparty/com/oracle/java-jre-linux-x64/20-b17/java-jre-linux-x64-20-b17.tgz")

  lazy val openmole = AssemblyProject("openmole", settings = tarProject ++ urlDownloadProject) settings (
    resourceOutDir := "",
    setExecutable ++= Seq("openmole", "openmole.bat"),
    resourcesAssemble <++= (assemble in openmolePlugins) map { f ⇒ Seq(f -> "plugins") },
    resourcesAssemble <++= (assemble in dbServer) map { f ⇒ Seq(f -> "dbserver") },
    resourcesAssemble <++= (assemble in consolePlugins) map { f ⇒ Seq(f -> "openmole-plugins") },
    resourcesAssemble <++= (assemble in guiPlugins) map { f ⇒ Seq(f -> "openmole-plugins-gui") },
    resourcesAssemble <++= (Tar.tar in openmoleRuntime) map { f ⇒ Seq(f -> "runtime") },
    downloads := Seq(java368URL -> "runtime/jvm-386.tar.gz", javax64URL -> "runtime/jvm-x64.tar.gz"),
    Tar.name := "openmole.tar.gz",
    Tar.innerFolder := "openmole",
    dependencyFilter := filter //DependencyFilter.fnToModuleFilter { m ⇒ m.organization == "org.eclipse.core" || m.organization == "fr.iscpif.gridscale.bundle" || m.organization == "org.bouncycastle" || m.organization == "org.openmole" }
  )

  lazy val coreDependencies = Seq(
    bouncyCastle,
    gridscale,
    logback,
    scopt,
    guava,
    bonecp,
    arm,
    xstream,
    slick,
    jline,
    Apache.ant,
    Apache.codec,
    Apache.config,
    Apache.exec,
    Apache.math,
    Apache.pool,
    Apache.log4j,
    Apache.sshd,
    groovy,
    h2,
    jasypt,
    jodaTime,
    scalaLang,
    slf4j
  )

  lazy val guiCoreDependencies = Seq(
    scalajsLibrary,
    scalajsTools,
    scalajsDom,
    scalaTags,
    autowire,
    upickle,
    rx,
    scalatra,
    jacksonJson,
    jetty,
    scalajHttp
  )

  lazy val openmolePlugins = AssemblyProject("openmoleplugins") settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "core") sendTo "",
    resourcesAssemble <++= Seq(openmoleui.project) sendTo "",
    libraryDependencies ++= Seq(
      Libraries.bouncyCastle,
      Libraries.gridscale,
      Libraries.logback,
      Libraries.scopt,
      Libraries.guava,
      Libraries.bonecp,
      Libraries.arm,
      Libraries.xstream,
      Libraries.slick,
      Libraries.jline,
      Apache.ant,
      Apache.codec,
      Apache.config,
      Apache.exec,
      Apache.math,
      Apache.pool,
      Apache.log4j,
      Apache.sshd,
      Libraries.jawn,
      Libraries.groovy,
      Libraries.h2,
      Libraries.jasypt,
      Libraries.jodaTime,
      Libraries.scalajHttp,
      Libraries.scalaLang,
      Libraries.scalatra,
      Libraries.scaladget,
      Libraries.slf4j,
      Libraries.robustIt,
      Libraries.jacksonJson,
      Libraries.jetty,
      Libraries.scalajsLibrary,
      Libraries.scalajsTools,
      Libraries.scalajsDom,
      Libraries.scalajsJQuery,
      // Libraries.autowireJVM,
      Libraries.scalaTags,
      Libraries.autowire,
      Libraries.upickle,
      // Libraries.upickleJVM,
      Libraries.rx
    ) ++ equinox,
      dependencyFilter := filter,
      dependencyNameMap := renameEquinox
  )

  lazy val consolePlugins = AssemblyProject("consoleplugins") settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "plugin", true) sendTo "",
    libraryDependencies ++=
    Seq(
      Apache.logging,
      opencsv,
      netlogo4,
      netlogo5,
      mgo,
      monocle,
      scalabc,
      gridscaleHTTP intransitive (),
      gridscalePBS intransitive (),
      gridscaleSLURM intransitive (),
      gridscaleDirac intransitive (),
      gridscaleGlite intransitive (),
      gridscaleSGE intransitive (),
      gridscaleCondor intransitive (),
      gridscalePBS intransitive (),
      gridscaleOAR intransitive (),
      gridscaleSSH intransitive ()
    ),
      dependencyFilter := filter
  )

  lazy val guiPlugins = AssemblyProject("guiplugins") settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a.contains("guiPlugin"), true) sendTo "",
    dependencyFilter := filter
  )

  lazy val dbServer = AssemblyProject("dbserver", "lib") settings (
    resourceOutDir := "bin",
    resourcesAssemble <++= Seq(Misc.replication.project, base.Runtime.dbserver.project) sendTo "lib",
    libraryDependencies ++= Seq(
      Libraries.xstream,
      Libraries.slick,
      Libraries.h2,
      Libraries.slf4j,
      Libraries.scalaLang
    ),
      dependencyFilter := filter
  )

  lazy val openmoleRuntime = AssemblyProject("runtime", "plugins", settings = tarProject) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "runtime") sendTo "plugins",
    setExecutable ++= Seq("run.sh"),
    Tar.name := "runtime.tar.gz",
    libraryDependencies ++= Seq(
      Libraries.logback,
      Libraries.scopt,
      Libraries.guava,
      Libraries.xstream,
      Libraries.slick,
      Libraries.gridscale,
      Apache.config,
      Apache.exec,
      Apache.math,
      Apache.pool,
      Apache.log4j,
      Libraries.h2,
      Libraries.jasypt,
      Libraries.jodaTime,
      Libraries.scalaLang,
      Libraries.slf4j,
      Libraries.groovy
    ) ++ equinox,
      dependencyFilter := filter,
      dependencyNameMap := renameEquinox
  )

  lazy val daemon = AssemblyProject("daemon", "plugins", settings = tarProject) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ (a contains "core") || (a contains "daemon")) sendTo "plugins",
    libraryDependencies ++= Seq(
      gridscale,
      gridscaleSSH,
      bouncyCastle
    ) ++ equinox ++ coreDependencies,
      setExecutable ++= Seq("openmole-daemon", "openmole-daemon.bat"),
      dependencyFilter := filter,
      dependencyNameMap := renameEquinox,
      Tar.name := "openmole-daemon.tar.gz",
      Tar.innerFolder := "openmole-daemon"
  )

  lazy val docProj = Project("documentation", dir / "documentation") aggregate ((Base.subProjects ++ Gui.subProjects ++ Web.subProjects): _*) settings (
    unidocSettings: _*
  ) settings (compile := Analysis.Empty,
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(Libraries.subProjects: _*) -- inProjects(ThirdParties.subProjects: _*)
    )

}
