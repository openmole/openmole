package root

import root.runtime.REST
import sbt._
import Keys._

import org.openmole.buildsystem.OMKeys._
import org.openmole.buildsystem._, Assembly._
import Libraries._
import com.typesafe.sbt.osgi.OsgiKeys
import sbt.inc.Analysis
import sbtunidoc.Plugin._
import UnidocKeys._

import scala.util.matching.Regex
import sbtbuildinfo.Plugin._

object Bin extends Defaults(Core, Plugin, Runtime, Gui, Libraries, ThirdParties, root.Doc) {
  val dir = file("bin")

  def filter(m: ModuleID) = {
    m.organization.startsWith("org.eclipse") ||
      m.organization == "fr.iscpif.gridscale.bundle" ||
      m.organization == "org.bouncycastle" ||
      m.organization.contains("org.openmole")
  }

  lazy val openmoleStartLevels =
    Seq(
      "org.eclipse.core.runtime" → 1,
      "org-openmole-core-logging" → 2
    )

  lazy val equinox = Seq(
    equinoxApp intransitive (),
    equinoxContenttype intransitive (),
    equinoxJobs intransitive (),
    equinoxRuntime intransitive (),
    equinoxCommon intransitive (),
    equinoxLauncher intransitive (),
    equinoxRegistry intransitive (),
    equinoxPreferences intransitive (),
    equinoxOSGi intransitive ()
  )

  def rename(m: ModuleID): String =
    if (m.name.startsWith("org.eclipse.equinox.launcher")) "org.eclipse.equinox.launcher.jar"
    else if (m.organization.startsWith("org.eclipse")) s"${m.organization}.${m.name}_${m.revision}.jar"
    else s"${m.name}.jar"

  lazy val openmoleUI = OsgiProject("org.openmole.ui", singleton = true, imports = Seq("*")) settings (
    organization := "org.openmole.ui",
    libraryDependencies += equinoxApp
  ) dependsOn (
      Runtime.console,
      Core.workspace,
      Core.replication,
      Core.exception,
      Core.tools,
      Core.event,
      Core.pluginManager,
      Core.workflow,
      Core.batch,
      gui.Server.core,
      gui.Client.core,
      gui.Bootstrap.js,
      gui.Bootstrap.osgi,
      Core.logging,
      runtime.REST.server,
      Core.console,
      Core.dsl
    )

  lazy val java368URL = new URL("http://maven.openmole.org/thirdparty/com/oracle/java-jre-linux-386/8-u45/java-jre-linux-386-8-u45.tgz")
  lazy val javax64URL = new URL("http://maven.openmole.org/thirdparty/com/oracle/java-jre-linux-x64/8-u45/java-jre-linux-x64-8-u45.tgz")

  import OMKeys.OSGiApplication._

  lazy val openmole =
    Project("openmole", dir / "openmole", settings = tarProject ++ assemblySettings ++ osgiApplicationSettings) settings (commonsSettings: _*) settings (
      setExecutable ++= Seq("openmole", "openmole.bat"),
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → p },
      resourcesAssemble <++= Seq(openmoleUI.project, Runtime.console.project, REST.server.project) sendTo {
        assemblyPath / "plugins"
      },
      resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (assemble in openmoleGUI, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (assemble in dbServer, assemblyPath) map { case (r, p) ⇒ r → (p / "dbserver") },
      resourcesAssemble <+= (assemble in consolePlugins, assemblyPath) map { case (r, p) ⇒ r → (p / "openmole-plugins") },
      resourcesAssemble <+= (assemble in guiPlugins, assemblyPath) map { case (r, p) ⇒ r → (p / "openmole-plugins-gui") },
      resourcesAssemble <+= (Tar.tar in openmoleRuntime, assemblyPath) map { case (r, p) ⇒ r → (p / "runtime") },
      downloads := Seq(java368URL → "runtime/jvm-386.tar.gz", javax64URL → "runtime/jvm-x64.tar.gz"),
      libraryDependencies += Libraries.scalajHttp,
      dependencyFilter := filter,
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      Tar.name := "openmole.tar.gz",
      Tar.innerFolder := "openmole",
      pluginsDirectory := assemblyPath.value / "plugins",
      header :=
      """|eclipse.application=org.openmole.ui
          |osgi.bundles.defaultStartLevel=4""".stripMargin,
      startLevels := openmoleStartLevels,
      config := assemblyPath.value / "configuration/config.ini",
      cleanFiles <++= cleanFiles in openmoleCore,
      cleanFiles <++= cleanFiles in openmoleGUI,
      cleanFiles <++= cleanFiles in consolePlugins,
      cleanFiles <++= cleanFiles in guiPlugins
    )

  lazy val webServerDependencies = Seq(
    scalatra intransitive (),
    bouncyCastle
  )

  lazy val coreDependencies = Seq[sbt.ModuleID](
    Libraries.gridscale,
    Libraries.logback,
    Libraries.scopt,
    Libraries.guava,
    Libraries.bonecp,
    Libraries.arm,
    Libraries.xstream,
    Libraries.slick,
    Libraries.ant,
    Libraries.codec,
    Libraries.apacheConfig,
    Libraries.exec,
    Libraries.math,
    Libraries.log4j,
    Libraries.h2,
    Libraries.jasypt,
    Libraries.jodaTime,
    Libraries.scalaLang,
    Libraries.slf4j
  ) ++ webServerDependencies

  lazy val guiCoreDependencies = Seq(
    scalajsLibrary,
    scalajsTools,
    scalajsDom,
    scalaTags,
    autowire,
    upickle,
    rx,
    scalatra intransitive (),
    scalajHttp,
    d3,
    bootstrap,
    jquery,
    ace,
    txtmark,
    scaladget
  )

  //FIXME separate web plugins from core ones
  lazy val openmoleCore = Project("openmolecore", dir / "target" / "openmolecore", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "core") sendTo assemblyPath,
    resourcesAssemble <++= Seq(Runtime.console.project) sendTo assemblyPath,
    libraryDependencies ++= coreDependencies,
    libraryDependencies ++= equinox,
    dependencyFilter := filter,
    dependencyName := rename
  )

  lazy val openmoleGUI = Project("openmoleGUI", dir / "target" / "openmoleGUI", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "gui") sendTo assemblyPath,
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "doc") sendTo assemblyPath,
    libraryDependencies ++= guiCoreDependencies,
    dependencyFilter := filter
  )

  lazy val consolePlugins = Project("consoleplugins", dir / "target" / "consoleplugins", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "plugin", true) sendTo assemblyPath,
    libraryDependencies ++=
    Seq(
      sshd,
      family,
      logging,
      opencsv,
      netlogo4,
      netlogo5,
      mgo,
      monocle,
      scalabc,
      groovy,
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

  lazy val guiPlugins = Project("guiplugins", dir / "target" / "guiplugins", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a.contains("guiPlugin"), true) sendTo assemblyPath,
    dependencyFilter := filter
  )

  lazy val dbServer = Project("dbserver", dir / "dbserver", settings = assemblySettings) settings (commonsSettings: _*) settings (
    assemblyDependenciesPath := assemblyPath.value / "lib",
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → (p / "bin") },
    resourcesAssemble <++= Seq(Core.replication.project, Runtime.dbserver.project) sendTo (assemblyPath / "lib"),
    libraryDependencies ++= Seq(
      Libraries.xstream,
      Libraries.slick,
      Libraries.h2,
      Libraries.slf4j,
      Libraries.scalaLang
    ),
    dependencyFilter := filter
  )

  lazy val openmoleRuntime = Project("runtime", dir / "runtime", settings = tarProject ++ assemblySettings ++ osgiApplicationSettings) settings (commonsSettings: _*) settings (
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → p },
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "runtime") sendTo (assemblyPath / "plugins"),
    setExecutable ++= Seq("run.sh"),
    Tar.name := "runtime.tar.gz",
    libraryDependencies ++= coreDependencies ++ equinox,
    dependencyFilter := filter,
    dependencyName := rename,
    pluginsDirectory := assemblyPath.value / "plugins",
    header :=
    """ |eclipse.application=org.openmole.runtime.runtime
        |osgi.bundles.defaultStartLevel=4""".stripMargin,
    startLevels := openmoleStartLevels,
    config := assemblyPath.value / "configuration/config.ini"
  )

  lazy val daemon = Project("daemon", dir / "daemon", settings = tarProject ++ assemblySettings ++ osgiApplicationSettings) settings (commonsSettings: _*) settings (
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    resourcesAssemble <++=
    Seq(Runtime.daemon.project, plugin.Environment.gridscale.project, plugin.Environment.desktopgrid.project, plugin.Tool.sftpserver.project) sendTo (assemblyPath / "plugins"),
    resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → p },
    libraryDependencies ++= Seq(
      Libraries.sshd,
      gridscale,
      gridscaleSSH,
      bouncyCastle
    ) ++ equinox ++ coreDependencies,
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    dependencyFilter := filter,
    setExecutable ++= Seq("openmole-daemon", "openmole-daemon.bat"),
    dependencyName := rename,
    Tar.name := "openmole-daemon.tar.gz",
    Tar.innerFolder := "openmole-daemon",
    pluginsDirectory := assemblyPath.value / "plugins",
    header :=
    """|eclipse.application=org.openmole.runtime.daemon
        |osgi.bundles.defaultStartLevel=4""".stripMargin,
    startLevels := openmoleStartLevels,
    config := assemblyPath.value / "configuration/config.ini"
  )

  lazy val api = Project("api", dir / "target" / "api") settings (commonsSettings: _*) settings (
    unidocSettings: _*
  ) settings (tarProject: _*) settings (
      compile := Analysis.Empty,
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(Core.subProjects ++ Plugin.subProjects: _*) -- inProjects(Libraries.subProjects ++ ThirdParties.subProjects: _*),
      Tar.name := "openmole-api.tar.gz",
      Tar.folder <<= (unidoc in Compile).map(_.head)
    )

  lazy val siteGeneration =
    OsgiProject(
      "org.openmole.site",
      singleton = true,
      imports = Seq("*"),
      settings = commonsSettings ++ scalatex.SbtPlugin.projectSettings ++ assemblySettings
    ) settings (
        OsgiKeys.bundle <<= OsgiKeys.bundle dependsOn (assemble),
        organization := "org.openmole.site",
        libraryDependencies += Libraries.xstream,
        libraryDependencies += Libraries.scalatexSite,
        libraryDependencies += Libraries.scalaLang,
        libraryDependencies += Libraries.equinoxApp,
        libraryDependencies += Libraries.jgit intransitive (),
        libraryDependencies += Libraries.txtmark,
        libraryDependencies += Libraries.toolxitBibtex intransitive (),
        resourcesAssemble <+= (Tar.tar in openmole, resourceManaged in Compile) map { case (f, d) ⇒ f → d },
        resourcesAssemble <+= (Tar.tar in daemon, resourceManaged in Compile) map { case (f, d) ⇒ f → d },
        resourcesAssemble <+= (Tar.tar in api, resourceManaged in Compile) map { case (doc, d) ⇒ doc → d },
        dependencyFilter := { _ ⇒ false }
      ) dependsOn (Runtime.console, Core.buildinfo, root.Doc.doc)

  lazy val site =
    Project("site", dir / "site", settings = assemblySettings ++ osgiApplicationSettings) settings (commonsSettings: _*) settings (
      setExecutable ++= Seq("site"),
      resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "doc") sendTo (assemblyPath / "plugins"),
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → p },
      resourcesAssemble <++= Seq(siteGeneration.project) sendTo (assemblyPath / "plugins"),
      resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (assemble in consolePlugins, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      dependencyFilter := filter,
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      dependencyName := rename,
      header :=
      """|eclipse.application=org.openmole.site
          |osgi.bundles.defaultStartLevel=4""".stripMargin,
      startLevels := openmoleStartLevels ++ Seq("openmole-plugin" → 3),
      pluginsDirectory := assemblyPath.value / "plugins",
      config := assemblyPath.value / "configuration/config.ini"
    ) dependsOn (siteGeneration, Core.tools)

}
