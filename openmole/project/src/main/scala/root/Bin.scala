package root

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

object Bin extends Defaults(Core, Plugin, Runtime, Gui, Libraries, ThirdParties, Web) {
  val dir = file("bin")

  def filter(m: ModuleID) = {
    m.organization == "org.eclipse.core" ||
      m.organization == "fr.iscpif.gridscale.bundle" ||
      m.organization == "org.bouncycastle" ||
      m.organization.contains("org.openmole")
  }

  lazy val openmoleStartLevels =
    Seq(
      "org.eclipse.core.runtime" -> 1,
      "org-openmole-misc-logging" -> 2
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

  lazy val renameEquinox =
    Map[Regex, String ⇒ String](
      """org\.eclipse\.equinox\.launcher.*\.jar""".r -> { s ⇒ "org.eclipse.equinox.launcher.jar" },
      """org\.eclipse\.(core|equinox|osgi)""".r -> { s ⇒ s.replaceFirst("-", "_") }
    )

  lazy val openmoleConsole = OsgiProject("org.openmole.console") settings (
    organization := "org.openmole.console"
  ) dependsOn (
      Core.workflow,
      Core.console,
      Core.dsl,
      Core.batch
    )

  lazy val openmoleUI = OsgiProject("org.openmole.ui", singleton = true) settings (
    organization := "org.openmole.ui",
    libraryDependencies += equinoxApp
  ) dependsOn (
      openmoleConsole,
      Core.workspace,
      Core.replication,
      Core.exception,
      Core.tools,
      Core.eventDispatcher,
      Core.pluginManager,
      Core.workflow,
      Core.batch,
      gui.Server.core,
      gui.Client.core,
      gui.Bootstrap.js,
      gui.Bootstrap.osgi,
      Core.logging,
      Web.core,
      Core.console,
      Core.dsl)

  lazy val java368URL = new URL("http://maven.iscpif.fr/thirdparty/com/oracle/java-jre-linux-386/20-b17/java-jre-linux-386-20-b17.tgz")
  lazy val javax64URL = new URL("http://maven.iscpif.fr/thirdparty/com/oracle/java-jre-linux-x64/20-b17/java-jre-linux-x64-20-b17.tgz")

  import OMKeys.OSGiApplication._

  lazy val openmole =
    Project("openmole", dir / "openmole", settings = tarProject ++ assemblySettings ++ osgiApplicationSettings) settings (commonsSettings: _*) settings (
      setExecutable ++= Seq("openmole", "openmole.bat"),
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r -> p },
      resourcesAssemble <++= Seq(openmoleUI.project, openmoleConsole.project) sendTo { assemblyPath / "plugins" },
      resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r -> p / "plugins" },
      resourcesAssemble <+= (assemble in openmoleGUI, assemblyPath) map { case (r, p) ⇒ r -> p / "plugins" },
      resourcesAssemble <+= (assemble in dbServer, assemblyPath) map { case (r, p) ⇒ r -> p / "dbserver" },
      resourcesAssemble <+= (assemble in consolePlugins, assemblyPath) map { case (r, p) ⇒ r -> p / "openmole-plugins" },
      resourcesAssemble <+= (assemble in guiPlugins, assemblyPath) map { case (r, p) ⇒ r -> p / "openmole-plugins-gui" },
      resourcesAssemble <+= (Tar.tar in openmoleRuntime, assemblyPath) map { case (r, p) ⇒ r -> p / "runtime" },
      downloads := Seq(java368URL -> "runtime/jvm-386.tar.gz", javax64URL -> "runtime/jvm-x64.tar.gz"),
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
      cleanFiles <+= baseDirectory { base ⇒ dir / "target" }
    )

  lazy val webServerDependencies = Seq(
    scalate
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
    Libraries.groovy,
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
    scalatra,
    jacksonJson,
    jetty,
    scalajHttp
  )

  //FIXME separate web plugins from core ones
  lazy val openmoleCore = Project("openmolecore", dir / "target" / "openmolecore", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "core") sendTo assemblyPath,
    resourcesAssemble <++= Seq(openmoleConsole.project) sendTo assemblyPath,
    libraryDependencies ++= coreDependencies,
    libraryDependencies ++= equinox,
    dependencyFilter := filter,
    dependencyNameMap := renameEquinox
  )

  lazy val openmoleGUI = Project("openmoleGUI", dir / "target" / "openmoleGUI", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "gui") sendTo assemblyPath,
    libraryDependencies ++= guiCoreDependencies,
    dependencyFilter := filter
  )

  lazy val consolePlugins = Project("consoleplugins", dir / "target" / "consoleplugins", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "plugin", true) sendTo assemblyPath,
    libraryDependencies ++=
    Seq(
      sshd,
      bouncyCastle,
      family,
      logging,
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

  lazy val guiPlugins = Project("guiplugins", dir / "target" / "guiplugins", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a.contains("guiPlugin"), true) sendTo assemblyPath,
    dependencyFilter := filter
  )

  lazy val dbServer = Project("dbserver", dir / "dbserver", settings = assemblySettings) settings (commonsSettings: _*) settings (
    assemblyDependenciesPath := assemblyPath.value / "lib",
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r -> p / "bin" },
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
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r -> p },
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "runtime") sendTo (assemblyPath / "plugins"),
    setExecutable ++= Seq("run.sh"),
    Tar.name := "runtime.tar.gz",
    libraryDependencies ++= coreDependencies ++ equinox,
    dependencyFilter := filter,
    dependencyNameMap := renameEquinox,
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
    Seq(Runtime.daemon.project, plugin.Environment.desktopgrid.project, plugin.Tool.sftpserver.project) sendTo (assemblyPath / "plugins"),
    resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r -> p / "plugins" },
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r -> p },
    libraryDependencies ++= Seq(
      Libraries.sshd,
      gridscale,
      gridscaleSSH,
      bouncyCastle
    ) ++ equinox ++ coreDependencies,
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      dependencyFilter := filter,
      setExecutable ++= Seq("openmole-daemon", "openmole-daemon.bat"),
      dependencyNameMap := renameEquinox,
      Tar.name := "openmole-daemon.tar.gz",
      Tar.innerFolder := "openmole-daemon",
      pluginsDirectory := assemblyPath.value / "plugins",
      header :=
      """|eclipse.application=org.openmole.runtime.daemon
         |osgi.bundles.defaultStartLevel=4""".stripMargin,
      startLevels := openmoleStartLevels,
      config := assemblyPath.value / "configuration/config.ini"
  )

  lazy val api = Project("api", dir / "target" / "api") aggregate ((Core.subProjects ++ Gui.subProjects ++ Web.subProjects): _*) settings (commonsSettings: _*) settings (
    unidocSettings: _*
  ) settings (tarProject: _*) settings (
      compile := Analysis.Empty,
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(Libraries.subProjects: _*) -- inProjects(ThirdParties.subProjects: _*),
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
        libraryDependencies += Libraries.scalatexSite,
        libraryDependencies += Libraries.scalaLang,
        libraryDependencies += Libraries.equinoxApp,
        resourcesAssemble <+= (Tar.tar in openmole, resourceManaged in Compile) map { case (f, d) ⇒ f -> d },
        resourcesAssemble <+= (Tar.tar in daemon, resourceManaged in Compile) map { case (f, d) ⇒ f -> d },
        resourcesAssemble <+= (Tar.tar in api, resourceManaged in Compile) map { case (doc, d) ⇒ doc -> d },
        dependencyFilter := { _ ⇒ false }
      ) settings (
          buildInfoSettings ++
            Seq(
              sourceGenerators in Compile <+= buildInfo,
              buildInfoKeys :=
                Seq[BuildInfoKey](
                  name,
                  version,
                  scalaVersion,
                  sbtVersion,
                  BuildInfoKey.action("buildTime") {
                    System.currentTimeMillis
                  }),
              buildInfoPackage := "org.openmole.site.buildinfo"
            ): _*
        ) dependsOn (openmoleConsole, ThirdParties.toolxitBibtex)

  lazy val site =
    Project("site", dir / "site", settings = assemblySettings ++ osgiApplicationSettings) settings (commonsSettings: _*) settings (
      setExecutable ++= Seq("site"),
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r -> p },
      resourcesAssemble <++= Seq(siteGeneration.project, ThirdParties.toolxitBibtex.project) sendTo (assemblyPath / "plugins"),
      resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r -> p / "plugins" },
      resourcesAssemble <+= (assemble in consolePlugins, assemblyPath) map { case (r, p) ⇒ r -> p / "plugins" },
      dependencyFilter := filter,
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      dependencyNameMap := renameEquinox,
      header :=
      """|eclipse.application=org.openmole.site
         |osgi.bundles.defaultStartLevel=4""".stripMargin,
      startLevels := openmoleStartLevels ++ Seq("openmole-plugin" -> 3),
      pluginsDirectory := assemblyPath.value / "plugins",
      config := assemblyPath.value / "configuration/config.ini"
    ) dependsOn (siteGeneration, Core.tools)

}
