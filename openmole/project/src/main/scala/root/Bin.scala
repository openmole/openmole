package root

import root.runtime.REST
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
    (m.organization.startsWith("org.eclipse") ||
      m.organization == "fr.iscpif.gridscale.bundle" ||
      m.organization == "org.bouncycastle" ||
      m.organization.contains("org.openmole") ||
      m.organization == "org.osgi") && m.name != "osgi"

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
    equinoxOSGi intransitive (),
    osgiCompendium intransitive ()
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
    Project("openmole", dir / "openmole", settings = tarProject ++ assemblySettings) settings (commonsSettings: _*) settings (
      setExecutable ++= Seq("openmole", "openmole.bat"),
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath).identityMap,
      resourcesAssemble <++= Seq(openmoleUI.project, Runtime.console.project, REST.server.project) sendTo { assemblyPath / "plugins" },
      resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (assemble in openmoleGUI, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (resourceDirectory in gui.Server.core in Compile, assemblyPath) map { case (r, p) ⇒ (r / "webapp") → (p / "webapp") },
      resourcesAssemble <+= (fullOptJS in gui.Client.core in Compile, assemblyPath) map { case (js, p) ⇒ js.data → (p / "webapp/js/openmole.js") },
      resourcesAssemble <+= (packageMinifiedJSDependencies in gui.Client.core in Compile, assemblyPath) map { case (js, p) ⇒ js → (p / "webapp/js/deps.js") },
      resourcesAssemble <+= (assemble in dbServer, assemblyPath) map { case (r, p) ⇒ r → (p / "dbserver") },
      resourcesAssemble <+= (assemble in consolePlugins, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (Tar.tar in openmoleRuntime, assemblyPath) map { case (r, p) ⇒ r → (p / "runtime" / r.getName) },
      resourcesAssemble <+= (assemble in launcher, assemblyPath) map { case (r, p) ⇒ r → (p / "launcher") },
      downloads := Seq(java368URL → "runtime/jvm-386.tar.gz", javax64URL → "runtime/jvm-x64.tar.gz"),
      libraryDependencies += Libraries.scalajHttp,
      libraryDependencies += Libraries.osgiCompendium,
      dependencyFilter := filter,
      dependencyName := rename,
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      Tar.name := "openmole.tar.gz",
      Tar.innerFolder := "openmole",
      pluginsDirectory := assemblyPath.value / "plugins",
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
    clapper,
    jquery
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
    dependencyName := rename
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

  lazy val openmoleRuntime =
    OsgiProject("org.openmole.runtime", singleton = true, imports = Seq("*"), settings = tarProject ++ assemblySettings) dependsOn (Core.workflow, Core.batch, Core.serializer, Core.logging, Core.event, Core.exception) settings (commonsSettings: _*) settings (
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → p },
      resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "runtime") sendTo (assemblyPath / "plugins"),
      resourcesAssemble <+= (assemble in launcher, assemblyPath) map { case (r, p) ⇒ r → (p / "launcher") },
      resourcesAssemble <+= (OsgiKeys.bundle, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins" / r.getName) },
      setExecutable ++= Seq("run.sh"),
      Tar.name := "runtime.tar.gz",
      libraryDependencies ++= coreDependencies,
      libraryDependencies += scopt,
      dependencyFilter := filter,
      dependencyName := rename,
      pluginsDirectory := assemblyPath.value / "plugins"
    )

  lazy val daemon = OsgiProject("org.openmole.daemon", settings = tarProject ++ assemblySettings) settings (commonsSettings: _*) dependsOn (Core.workflow, Core.workflow, Core.batch, Core.workspace,
    Core.fileService, Core.exception, Core.tools, Core.logging, plugin.Environment.desktopgrid) settings (
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      resourcesAssemble <++=
      Seq(plugin.Environment.gridscale.project, plugin.Environment.desktopgrid.project, plugin.Tool.sftpserver.project) sendTo (assemblyPath / "plugins"),
      resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (OsgiKeys.bundle, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins" / r.getName) },
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → p },
      resourcesAssemble <+= (assemble in launcher, assemblyPath) map { case (r, p) ⇒ r → (p / "launcher") },
      libraryDependencies ++= Seq(
        Libraries.sshd,
        gridscale,
        gridscaleSSH,
        bouncyCastle,
        scalaLang,
        logging,
        jodaTime,
        scopt
      ) ++ coreDependencies,
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      dependencyFilter := filter,
      dependencyName := rename,
      setExecutable ++= Seq("openmole-daemon", "openmole-daemon.bat"),
      dependencyName := rename,
      Tar.name := "openmole-daemon.tar.gz",
      Tar.innerFolder := "openmole-daemon"
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
        resourcesAssemble <+= (Tar.tar in openmole, resourceManaged in Compile) map { case (f, d) ⇒ f → (d / f.getName) },
        resourcesAssemble <+= (Tar.tar in daemon, resourceManaged in Compile) map { case (f, d) ⇒ f → (d / f.getName) },
        resourcesAssemble <+= (Tar.tar in api, resourceManaged in Compile) map { case (doc, d) ⇒ doc → (d / doc.getName) },
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
      startLevels := openmoleStartLevels ++ Seq("plugin" → 3),
      pluginsDirectory := assemblyPath.value / "plugins",
      config := assemblyPath.value / "configuration/config.ini"
    ) dependsOn (siteGeneration, Core.tools)

  lazy val launcher = OsgiProject("org.openmole.launcher", imports = Seq("*"), settings = assemblySettings) settings (
    autoScalaLibrary := false,
    libraryDependencies += equinoxOSGi,
    resourcesAssemble <+= (OsgiKeys.bundle, assemblyPath) map { case (f, d) ⇒ f → (d / f.getName) }
  )

}
