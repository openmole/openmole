package root

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import sbt._
import Keys._

import org.openmole.buildsystem.OMKeys._
import org.openmole.buildsystem._, Assembly._
import root.Libraries._
import com.typesafe.sbt.osgi.OsgiKeys
import sbt.inc.Analysis
import sbtunidoc.Plugin._
import UnidocKeys._

object Bin extends Defaults(Core, Plugin, Runtime, Gui, Libraries, ThirdParties, root.Doc) {
  val dir = file("bin")

  def filter(m: ModuleID) =
    m.organization.startsWith("org.eclipse") ||
      m.organization == "fr.iscpif.gridscale.bundle" ||
      m.organization == "org.bouncycastle" ||
      m.organization.contains("org.openmole")

  def pluginFilter(m: ModuleID) = m.name != "osgi" && m.name != "scala-library"

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
    else if (m.name.exists(_ == '-') == false) s"${m.organization.replaceAllLiterally(".", "-")}-${m.name}_${m.revision}.jar"
    else s"${m.name}_${m.revision}.jar"

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
      Core.logging,
      runtime.REST.server,
      Core.console,
      Core.dsl
    )

  lazy val java368URL = new URL("https://maven.openmole.org/thirdparty/com/oracle/java-jre-linux-386/8-u45/java-jre-linux-386-8-u45.tgz")
  lazy val javax64URL = new URL("https://maven.openmole.org/thirdparty/com/oracle/java-jre-linux-x64/8-u45/java-jre-linux-x64-8-u45.tgz")

  import OMKeys.OSGiApplication._

  lazy val openmole =
    Project("openmole", dir / "openmole", settings = tarProject ++ assemblySettings ++ osgiApplicationSettings) settings (commonsSettings: _*) settings (
      setExecutable ++= Seq("openmole", "openmole.bat"),
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → p },
      buildJS <<= (fullOptJS in openmoleGUI in Compile) map { _.data },
      resourcesAssemble <+= (buildJS, assemblyPath) map { case (js, p) ⇒ js → (p / "webapp") },
      resourcesAssemble <+= (buildJS, assemblyPath) map { case (js, p) ⇒ new File(js.getParent, js.getName.replace("opt.js", "jsdeps.min.js")) → (p / "webapp") },
      resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (assemble in openmoleGUI, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (assemble in dbServer, assemblyPath) map { case (r, p) ⇒ r → (p / "dbserver") },
      resourcesAssemble <+= (assemble in consolePlugins, assemblyPath) map { case (r, p) ⇒ r → (p / "openmole-plugins") },
      resourcesAssemble <+= (Tar.tar in openmoleRuntime, assemblyPath) map { case (r, p) ⇒ r → (p / "runtime") },
      downloads := Seq(java368URL → "runtime/jvm-386.tar.gz", javax64URL → "runtime/jvm-x64.tar.gz"),
      libraryDependencies += Libraries.scalajHttp,
      dependencyFilter := filter,
      dependencyName := rename,
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
      cleanFiles <++= cleanFiles in dbServer,
      cleanFiles <++= cleanFiles in openmoleRuntime
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
    Libraries.slf4j,
    Libraries.scalaz
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
    txtmark,
    scaladget,
    clapper
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
    dependencyFilter := filter,
    dependencyName := rename,
    jsDependencies += ace / s"$aceVersion/src-min/ace.js",
    jsDependencies += bootstrap / s"$bootsrapVersion/js/bootstrap.min.js",
    jsDependencies += d3 / s"$d3Version/d3.min.js",
    jsDependencies += tooltipster / s"$tooltipserVersion/js/jquery.tooltipster.min.js",
    jsDependencies += jquery / s"$jqueryVersion/jquery.min.js"
  ) enablePlugins (ScalaJSPlugin)

  lazy val consolePlugins = Project("consoleplugins", dir / "target" / "consoleplugins", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "plugin", true) sendTo assemblyPath,
    libraryDependencies ++=
    Seq(
      monocle intransitive (),
      sshd intransitive (),
      family intransitive (),
      logging intransitive (),
      opencsv intransitive (),
      netlogo4 intransitive (),
      netlogo5 intransitive (),
      mgo intransitive (),
      scalabc intransitive (),
      gridscaleHTTP intransitive (),
      gridscalePBS intransitive (),
      gridscaleSLURM intransitive (),
      gridscaleGlite intransitive (),
      gridscaleSGE intransitive (),
      gridscaleCondor intransitive (),
      gridscalePBS intransitive (),
      gridscaleOAR intransitive (),
      gridscaleSSH intransitive ()
    ) ++ apacheHTTP map (_ intransitive ()),
      dependencyFilter := pluginFilter,
      dependencyName := rename
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
    dependencyFilter := filter,
    dependencyName := rename
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
    dependencyName := rename,
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
        OsgiKeys.exportPackage := Seq("scalatex.openmole.*") ++ OsgiKeys.exportPackage.value,
        libraryDependencies += Libraries.xstream,
        libraryDependencies += Libraries.scalatexSite,
        libraryDependencies += Libraries.scalaLang,
        libraryDependencies ++= equinox,
        libraryDependencies += Libraries.jgit intransitive (),
        libraryDependencies += Libraries.txtmark,
        libraryDependencies += Libraries.toolxitBibtex intransitive (),
        resourcesAssemble <+= (Tar.tar in openmole, resourceManaged in Compile) map { case (f, d) ⇒ f → d },
        resourcesAssemble <+= (Tar.tar in daemon, resourceManaged in Compile) map { case (f, d) ⇒ f → d },
        resourcesAssemble <+= (Tar.tar in api, resourceManaged in Compile) map { case (doc, d) ⇒ doc → d },
        dependencyFilter := { _ ⇒ false }
      ) dependsOn (Core.project, Core.buildinfo, root.Doc.doc)

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
