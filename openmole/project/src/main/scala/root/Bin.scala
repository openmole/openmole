package root

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport._
import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._
import org.openmole.buildsystem._
import Assembly._
import root.Libraries._
import com.typesafe.sbt.osgi.OsgiKeys
import sbt.inc.Analysis
import sbtunidoc.Plugin._
import UnidocKeys._

object Bin extends Defaults(Core, Plugin, REST, Gui, Libraries, ThirdParties, root.Doc) {
  val dir = file("bin")

  def filter(m: ModuleID) =
    (m.organization == "fr.iscpif.gridscale.bundle" ||
      m.organization == "org.bouncycastle" ||
      m.organization.contains("org.openmole") ||
      (m.organization == "org.osgi" && m.name != "osgi"))

  def pluginFilter(m: ModuleID) = m.name != "osgi" && m.name != "scala-library"

  def rename(m: ModuleID): String =
    if (m.name.exists(_ == '-') == false) s"${m.organization.replaceAllLiterally(".", "-")}-${m.name}_${m.revision}.jar"
    else s"${m.name}_${m.revision}.jar"

  lazy val openmoleUI = OsgiProject("org.openmole.ui", singleton = true, imports = Seq("*")) settings (
    organization := "org.openmole.ui"
  ) dependsOn (
      console,
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
      REST.server,
      Core.console,
      Core.dsl
    )

  lazy val openmole =
    Project("openmole", dir / "openmole", settings = tarProject ++ assemblySettings) settings (commonsSettings: _*) settings (
      setExecutable ++= Seq("openmole", "openmole.bat"),
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath).identityMap,
      resourcesAssemble <++= Seq(openmoleUI.project, console.project, REST.server.project) sendTo { assemblyPath / "plugins" },
      resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (assemble in openmoleGUI, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (resourceDirectory in gui.Server.core in Compile, assemblyPath) map { case (r, p) ⇒ (r / "webapp") → (p / "webapp") },
      resourcesAssemble <+= (fullOptJS in gui.Client.core in Compile, assemblyPath) map { case (js, p) ⇒ js.data → (p / "webapp/js/openmole.js") },
      resourcesAssemble <+= (packageMinifiedJSDependencies in gui.Client.core in Compile, assemblyPath) map { case (js, p) ⇒ js → (p / "webapp/js/deps.js") },
      resourcesAssemble <+= (assemble in dbServer, assemblyPath) map { case (r, p) ⇒ r → (p / "dbserver") },
      resourcesAssemble <+= (assemble in consolePlugins, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
      resourcesAssemble <+= (Tar.tar in openmoleRuntime, assemblyPath) map { case (r, p) ⇒ r → (p / "runtime" / r.getName) },
      resourcesAssemble <+= (assemble in launcher, assemblyPath) map { case (r, p) ⇒ r → (p / "launcher") },
      libraryDependencies += Libraries.scalajHttp,
      dependencyFilter := filter,
      dependencyName := rename,
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      Tar.name := "openmole.tar.gz",
      Tar.innerFolder := "openmole",
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
    Libraries.scalaz,
    Libraries.asm,
    Libraries.collections,
    Libraries.configuration,
    Libraries.logging
  ) ++ webServerDependencies

  lazy val guiCoreDependencies = Seq(
    scalajsTools,
    scalaTags,
    autowire,
    upickle,
    scalatra intransitive (),
    scalajHttp,
    txtmark,
    clapper,
    rx,
    scalajs
  )

  //FIXME separate web plugins from core ones
  lazy val openmoleCore = Project("openmolecore", dir / "target" / "openmolecore", settings = assemblySettings) settings (commonsSettings: _*) settings (
    resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "core") sendTo assemblyPath,
    resourcesAssemble <++= Seq(console.project) sendTo assemblyPath,
    libraryDependencies ++= coreDependencies,
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
      opencsv intransitive (),
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
      gridscaleSSH intransitive (),
      osgiCompendium intransitive ()
    ) ++ apacheHTTP map (_ intransitive ()),
      dependencyFilter := pluginFilter,
      dependencyName := rename
  )

  lazy val dbServer = OsgiProject("org.openmole.dbserver", settings = assemblySettings) settings (commonsSettings: _*) dependsOn (Core.replication) settings (
    assemblyDependenciesPath := assemblyPath.value / "lib",
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → (p / "bin") },
    resourcesAssemble <++= Seq(Core.replication.project) sendTo (assemblyPath / "lib"),
    resourcesAssemble <+= (OsgiKeys.bundle, assemblyPath) map { case (r, p) ⇒ r → (p / "lib" / r.getName) },
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
      dependencyName := rename
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

  lazy val site =
    OsgiProject(
      "org.openmole.site",
      singleton = true,
      imports = Seq("*"),
      settings = commonsSettings ++ scalatex.SbtPlugin.projectSettings ++ assemblySettings
    ) settings (
        organization := "org.openmole.site",
        OsgiKeys.exportPackage := Seq("scalatex.openmole.*") ++ OsgiKeys.exportPackage.value,
        libraryDependencies += Libraries.xstream,
        libraryDependencies += Libraries.scalatexSite,
        libraryDependencies += Libraries.scalaTags,
        libraryDependencies += Libraries.upickle,
        libraryDependencies += Libraries.scalaLang,
        libraryDependencies += Libraries.txtmark,
        libraryDependencies += Libraries.toolxitBibtex intransitive (),
        setExecutable ++= Seq("site"),
        assemblyDependenciesPath := assemblyPath.value / "plugins",
        resourcesAssemble <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "doc") sendTo (assemblyPath / "plugins"),
        resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ (r / "site") → (p / "site") },
        resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → (p / "resources") },
        resourcesAssemble <+= (OsgiKeys.bundle, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins" / r.getName) },
        resourcesAssemble <+= (assemble in openmoleCore, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
        resourcesAssemble <+= (assemble in consolePlugins, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins") },
        resourcesAssemble <+= (assemble in launcher, assemblyPath) map { case (r, p) ⇒ r → (p / "launcher") },
        resourcesAssemble <+= (Tar.tar in openmole, assemblyPath) map { case (f, d) ⇒ f → (d / "resources" / f.getName) },
        resourcesAssemble <+= (Tar.tar in daemon, assemblyPath) map { case (f, d) ⇒ f → (d / "resources" / f.getName) },
        resourcesAssemble <+= (Tar.tar in api, assemblyPath) map { case (doc, d) ⇒ doc → (d / "resources" / doc.getName) },
        dependencyFilter := filter,
        dependencyName := rename
      ) dependsOn (Core.project, Core.buildinfo, root.Doc.doc)

  lazy val launcher = OsgiProject("org.openmole.launcher", imports = Seq("*"), settings = assemblySettings) settings (
    autoScalaLibrary := false,
    libraryDependencies += equinoxOSGi,
    resourcesAssemble <+= (OsgiKeys.bundle, assemblyPath) map { case (f, d) ⇒ f → (d / f.getName) }
  )

  lazy val console = OsgiProject("org.openmole.console", imports = Seq("*")) settings (
    libraryDependencies += upickle
  ) dependsOn (
      Core.workflow,
      Core.console,
      Core.project,
      Core.dsl,
      Core.batch,
      Core.buildinfo
    )

}
