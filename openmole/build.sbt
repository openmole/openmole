


import org.openmole.buildsystem._
import OMKeys._

import sbt._
import Keys._

organization := "org.openmole"
name := "openmole-root"

def macroParadise =
  addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)

lazy val scalaVersionValue = "2.11.8"

def defaultSettings = BuildSystem.settings ++
  Seq(
    organization := "org.openmole",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalaVersion in Global := scalaVersionValue,
    scalacOptions ++= Seq("-target:jvm-1.8", "-language:higherKinds"),
    scalacOptions ++= Seq("-Xmax-classfile-name", "140"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    publishArtifact in (packageDoc in install) := false,
    publishArtifact in (packageSrc in install) := false,
    addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.fullMapped(_ ⇒ scalaVersionValue))
  )


publishTo in ThisBuild <<= isSnapshot(if (_) Some("OpenMOLE Nexus" at "https://maven.openmole.org/snapshots") else Some("OpenMOLE Nexus" at "https://maven.openmole.org/releases"))


/* ------ Third parties ---------- */

def thirdPartiesDir = file("third-parties")
def thirdPartiesSettings = defaultSettings ++ Seq(libraryDependencies += Libraries.scalatest)
def allThirdParties = Seq(openmoleCache, openmoleTar, openmoleFile, openmoleLock, openmoleLogger, openmoleThread, openmoleHash, openmoleStream, openmoleCollection, openmoleCrypto, openmoleStatistics, openmoleTypes, openmoleByteCode, openmoleOSGi, txtmark)

lazy val openmoleCache = OsgiProject(thirdPartiesDir, "org.openmole.tool.cache", imports = Seq("*")) dependsOn (openmoleLogger) settings(thirdPartiesSettings: _*)
lazy val openmoleTar = OsgiProject(thirdPartiesDir, "org.openmole.tool.tar", imports = Seq("*")) dependsOn (openmoleFile) settings(thirdPartiesSettings: _*)
lazy val openmoleFile = OsgiProject(thirdPartiesDir, "org.openmole.tool.file", imports = Seq("*")) dependsOn (openmoleLock, openmoleStream, openmoleStream, openmoleLogger) settings(thirdPartiesSettings: _*)
lazy val openmoleLock = OsgiProject(thirdPartiesDir, "org.openmole.tool.lock", imports = Seq("*")) settings(thirdPartiesSettings: _*)
lazy val openmoleLogger = OsgiProject(thirdPartiesDir, "org.openmole.tool.logger", imports = Seq("*")) settings(thirdPartiesSettings: _*)
lazy val openmoleThread = OsgiProject(thirdPartiesDir, "org.openmole.tool.thread", imports = Seq("*")) dependsOn (openmoleLogger) settings(thirdPartiesSettings: _*)
lazy val openmoleHash = OsgiProject(thirdPartiesDir, "org.openmole.tool.hash", imports = Seq("*")) dependsOn (openmoleFile, openmoleStream) settings(thirdPartiesSettings: _*)
lazy val openmoleStream = OsgiProject(thirdPartiesDir, "org.openmole.tool.stream", imports = Seq("*")) dependsOn (openmoleThread) settings (libraryDependencies += Libraries.collections) settings(thirdPartiesSettings: _*)
lazy val openmoleCollection = OsgiProject(thirdPartiesDir, "org.openmole.tool.collection", imports = Seq("*"))  settings (libraryDependencies += Libraries.scalaLang) settings(thirdPartiesSettings: _*)
lazy val openmoleCrypto = OsgiProject(thirdPartiesDir, "org.openmole.tool.crypto", imports = Seq("*"))  settings (libraryDependencies += Libraries.bouncyCastle) settings(thirdPartiesSettings: _*)
lazy val openmoleStatistics = OsgiProject(thirdPartiesDir, "org.openmole.tool.statistics", imports = Seq("*"))  dependsOn (openmoleLogger) settings(thirdPartiesSettings: _*)
lazy val openmoleTypes = OsgiProject(thirdPartiesDir, "org.openmole.tool.types", imports = Seq("*")) settings (libraryDependencies += Libraries.scalaz) settings(thirdPartiesSettings: _*)
lazy val openmoleByteCode = OsgiProject(thirdPartiesDir, "org.openmole.tool.bytecode", imports = Seq("*"))  settings (libraryDependencies += Libraries.asm) settings(thirdPartiesSettings: _*)
lazy val openmoleOSGi = OsgiProject(thirdPartiesDir, "org.openmole.tool.osgi", imports = Seq("*"))  dependsOn (openmoleFile) settings(libraryDependencies += Libraries.equinoxOSGi) settings(thirdPartiesSettings: _*)
lazy val txtmark = OsgiProject(thirdPartiesDir, "com.quandora.txtmark", exports = Seq("com.github.rjeschke.txtmark.*"), imports = Seq("*")) settings(thirdPartiesSettings: _*)


/* ------------- Core ----------- */

def coreDir = file("core")
def coreSettings =
  defaultSettings ++
    osgiSettings ++
    Seq(
      Osgi.openMOLEScope := Some("provided"),
      libraryDependencies += Libraries.scalatest,
      libraryDependencies += Libraries.equinoxOSGi
    )

def allCore = Seq(workflow, serializer, communication, openmoleDSL, exception, tools, event, replication, workspace, macros, pluginManager, updater, fileService, logging, output, console, project, buildinfo, module)


lazy val workflow = OsgiProject(coreDir, "org.openmole.core.workflow", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.scalaLang, Libraries.math, Libraries.scalaz, Libraries.equinoxOSGi)
) dependsOn
  (event, exception, tools, updater, workspace, macros, pluginManager, serializer, output, console, replication % "test") settings(coreSettings: _*)

lazy val serializer = OsgiProject(coreDir, "org.openmole.core.serializer", global = true, imports = Seq("*")) settings (
  libraryDependencies += Libraries.xstream,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn (workspace, pluginManager, fileService, tools, openmoleTar, console) settings(coreSettings: _*)

lazy val communication = OsgiProject(coreDir, "org.openmole.core.communication", imports = Seq("*")) dependsOn (workflow, workspace) settings(coreSettings: _*)

lazy val openmoleDSL = OsgiProject(coreDir, "org.openmole.core.dsl", imports = Seq("*")) dependsOn (workflow, logging) settings(coreSettings: _*)

lazy val exception = OsgiProject(coreDir, "org.openmole.core.exception", imports = Seq("*")) settings(coreSettings: _*)

lazy val tools = OsgiProject(coreDir, "org.openmole.core.tools", global = true, imports = Seq("*")) settings
  (libraryDependencies ++= Seq(Libraries.xstream, Libraries.exec, Libraries.math, Libraries.scalaLang, Libraries.scalatest, Libraries.equinoxOSGi)) dependsOn
  (exception, openmoleTar, openmoleFile, openmoleLock, openmoleThread, openmoleHash, openmoleLogger, openmoleStream, openmoleCollection, openmoleStatistics, openmoleTypes, openmoleCache) settings(coreSettings: _*)

lazy val event = OsgiProject(coreDir, "org.openmole.core.event", imports = Seq("*")) dependsOn (tools) settings(coreSettings: _*)

lazy val replication = OsgiProject(coreDir, "org.openmole.core.replication", imports = Seq("*")) settings (bundleType += "dbserver",
  libraryDependencies ++= Seq(Libraries.slick, Libraries.xstream)) settings(coreSettings: _*)

lazy val workspace = OsgiProject(coreDir, "org.openmole.core.workspace", imports = Seq("*")) settings
  (libraryDependencies ++= Seq(Libraries.jasypt, Libraries.xstream, Libraries.math, Libraries.configuration)) dependsOn
  (exception, event, tools, replication, openmoleCrypto) settings(coreSettings: _*)

lazy val macros = OsgiProject(coreDir, "org.openmole.core.macros", imports = Seq("*")) settings (libraryDependencies += Libraries.scalaLang) settings(coreSettings: _*)

lazy val pluginManager = OsgiProject(
  coreDir,
  "org.openmole.core.pluginmanager",
  bundleActivator = Some("org.openmole.core.pluginmanager.internal.Activator"), imports = Seq("*")
) dependsOn (exception, tools, workspace) settings(coreSettings: _*)

lazy val updater = OsgiProject(coreDir, "org.openmole.core.updater", imports = Seq("*")) dependsOn (exception, tools, workspace) settings(coreSettings: _*)

lazy val fileService = OsgiProject(coreDir, "org.openmole.core.fileservice", imports = Seq("*")) dependsOn (tools, updater, workspace, openmoleTar) settings(coreSettings: _*)

lazy val module = OsgiProject(coreDir, "org.openmole.core.module", imports = Seq("*")) dependsOn (buildinfo, openmoleHash, openmoleFile, pluginManager) settings(coreSettings: _*) settings (
  libraryDependencies += Libraries.gridscaleHTTP,
  libraryDependencies += Libraries.json4s)

lazy val logging = OsgiProject(
   coreDir,
  "org.openmole.core.logging",
  bundleActivator = Some("org.openmole.core.logging.internal.Activator"), imports = Seq("*")
) settings (libraryDependencies ++= Seq(Libraries.log4j, Libraries.logback, Libraries.slf4j)) dependsOn (tools) settings(coreSettings: _*)

lazy val output = OsgiProject(coreDir, "org.openmole.core.output", imports = Seq("*")) dependsOn (openmoleStream) settings(coreSettings: _*)

lazy val console = OsgiProject(coreDir, "org.openmole.core.console", bundleActivator = Some("org.openmole.core.console.Activator"), global = true, imports = Seq("*")) dependsOn
  (pluginManager) settings (
    OsgiKeys.importPackage := Seq("*"),
    libraryDependencies += Libraries.scalaLang,
    libraryDependencies += Libraries.monocle,
    macroParadise
  ) dependsOn (openmoleByteCode, openmoleOSGi) settings(coreSettings: _*)

lazy val project = OsgiProject(coreDir, "org.openmole.core.project", imports = Seq("*")) dependsOn (console, openmoleDSL) settings (OsgiKeys.importPackage := Seq("*")) settings(coreSettings: _*)

lazy val buildinfo = OsgiProject(coreDir, "org.openmole.core.buildinfo", imports = Seq("*")) enablePlugins (ScalaJSPlugin) settings (buildInfoSettings: _*) settings (
  sourceGenerators in Compile <+= buildInfo,
  buildInfoKeys :=
  Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    BuildInfoKey.action("buildTime") { System.currentTimeMillis }
  ),
    buildInfoPackage := s"org.openmole.core.buildinfo",
    libraryDependencies += Libraries.gridscaleHTTP,
    libraryDependencies += Libraries.json4s
) settings(coreSettings: _*)


/* ------------- Plugins ----------- */


def pluginDir = file("plugins")
def allPlugin =
  allTask ++
    allSource ++
    allSampling ++
    allMethod ++
    allHook ++
    allGrouping ++
    allEnvironment ++
    allDomain ++
    allTools

def allTools = Seq(netLogoAPI, netLogo5API, csv, pattern, sftpserver)

lazy val defaultActivator = OsgiKeys.bundleActivator <<= (name) { n ⇒ Some(n + ".Activator") }

def pluginSettings =
  defaultSettings ++  Seq(
    defaultActivator,
    libraryDependencies += Libraries.equinoxOSGi,
    libraryDependencies += Libraries.scalatest
  )


/* Tools */

def toolsSettings = defaultSettings ++ Seq(OsgiKeys.bundleActivator := None, libraryDependencies += Libraries.scalatest)



lazy val netLogoAPI = OsgiProject(pluginDir, "org.openmole.plugin.tool.netlogo", imports = Seq("*")) settings (
  autoScalaLibrary := false,
  crossPaths := false
  ) settings(toolsSettings: _*)


lazy val netLogo5API = OsgiProject(pluginDir, "org.openmole.plugin.tool.netlogo5", imports = Seq("*")) dependsOn (netLogoAPI) settings (
  crossPaths := false,
  autoScalaLibrary := false,
  libraryDependencies += Libraries.netlogo5 intransitive (),
  libraryDependencies -= Libraries.scalatest
) settings(toolsSettings: _*)

lazy val csv = OsgiProject(pluginDir, "org.openmole.plugin.tool.csv", imports = Seq("*")) dependsOn (exception, openmoleDSL) settings (
  libraryDependencies += Libraries.opencsv,
  defaultActivator
) settings(toolsSettings: _*)

lazy val pattern = OsgiProject(pluginDir, "org.openmole.plugin.tool.pattern", imports = Seq("*")) dependsOn (exception, openmoleDSL) settings (defaultActivator) settings(toolsSettings: _*)

lazy val sftpserver = OsgiProject(pluginDir, "org.openmole.plugin.tool.sftpserver", imports = Seq("*")) dependsOn (tools) settings (libraryDependencies += Libraries.sshd) settings(toolsSettings: _*)


/* Domain */

def allDomain = Seq(collectionDomain, distributionDomain, fileDomain, modifierDomain, rangeDomain)

lazy val collectionDomain = OsgiProject(pluginDir,"org.openmole.plugin.domain.collection", imports = Seq("*")) dependsOn (openmoleDSL) settings(pluginSettings: _*)

lazy val distributionDomain = OsgiProject(pluginDir,"org.openmole.plugin.domain.distribution", imports = Seq("*")) dependsOn (openmoleDSL) settings
  (libraryDependencies ++= Seq(Libraries.math)) settings(pluginSettings: _*)

lazy val fileDomain = OsgiProject(pluginDir,"org.openmole.plugin.domain.file", imports = Seq("*")) dependsOn (openmoleDSL) settings(pluginSettings: _*)

lazy val modifierDomain = OsgiProject(pluginDir,"org.openmole.plugin.domain.modifier", imports = Seq("*")) dependsOn (openmoleDSL) settings (
  libraryDependencies += Libraries.scalatest
) settings(pluginSettings: _*)

lazy val rangeDomain = OsgiProject(pluginDir,"org.openmole.plugin.domain.range", imports = Seq("*")) dependsOn (openmoleDSL) settings(pluginSettings: _*)


/* Environment */

def allEnvironment = Seq(batch, oar, desktopgrid, egi, gridscale, pbs, sge, condor, slurm, ssh)

lazy val batch = OsgiProject(pluginDir, "org.openmole.plugin.environment.batch", imports = Seq("*")) dependsOn (
  workflow, workspace, tools, event, replication, updater, exception,
  serializer, fileService, pluginManager, openmoleTar, communication
) settings (
    libraryDependencies ++= Seq(
      Libraries.gridscale,
      Libraries.h2,
      Libraries.guava,
      Libraries.jasypt,
      Libraries.slick
    )
  ) settings(pluginSettings: _*)

lazy val oar = OsgiProject(pluginDir, "org.openmole.plugin.environment.oar", imports = Seq("*")) dependsOn (openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleOAR) settings(pluginSettings: _*)

lazy val desktopgrid = OsgiProject(pluginDir, "org.openmole.plugin.environment.desktopgrid", imports = Seq("*")) dependsOn (
  openmoleDSL,
  batch, sftpserver, gridscale
) settings(pluginSettings: _*)

lazy val egi = OsgiProject(pluginDir, "org.openmole.plugin.environment.egi", imports = Seq("!org.apache.http.*", "!fr.iscpif.gridscale.libraries.srmstub", "!fr.iscpif.gridscale.libraries.lbstub", "!fr.iscpif.gridscale.libraries.wmsstub", "!com.google.common.cache", "*")) dependsOn (openmoleDSL,
  updater, batch,
  workspace, fileService, gridscale) settings (
    libraryDependencies ++= Seq(Libraries.gridscaleGlite, Libraries.gridscaleHTTP, Libraries.scalaLang)
  ) settings(pluginSettings: _*)

lazy val gridscale = OsgiProject(pluginDir, "org.openmole.plugin.environment.gridscale", imports = Seq("*")) dependsOn (openmoleDSL, tools,
  batch, exception) settings(pluginSettings: _*)

lazy val pbs = OsgiProject(pluginDir, "org.openmole.plugin.environment.pbs", imports = Seq("*")) dependsOn (openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscalePBS) settings(pluginSettings: _*)

lazy val sge = OsgiProject(pluginDir, "org.openmole.plugin.environment.sge", imports = Seq("*")) dependsOn (openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleSGE) settings(pluginSettings: _*)

lazy val condor = OsgiProject(pluginDir, "org.openmole.plugin.environment.condor", imports = Seq("*")) dependsOn (openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleCondor) settings(pluginSettings: _*)

lazy val slurm = OsgiProject(pluginDir, "org.openmole.plugin.environment.slurm", imports = Seq("*")) dependsOn (openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleSLURM) settings(pluginSettings: _*)

lazy val ssh = OsgiProject(pluginDir, "org.openmole.plugin.environment.ssh", imports = Seq("*")) dependsOn (openmoleDSL, event, batch, gridscale) settings
  (libraryDependencies += Libraries.gridscaleSSH) settings(pluginSettings: _*)


/* Grouping */

def allGrouping = Seq(batchGrouping, onvariableGrouping)

lazy val batchGrouping = OsgiProject(pluginDir, "org.openmole.plugin.grouping.batch", imports = Seq("*")) dependsOn (exception, workflow, workspace) settings(pluginSettings: _*)

lazy val onvariableGrouping = OsgiProject(pluginDir, "org.openmole.plugin.grouping.onvariable", imports = Seq("*")) dependsOn (exception, workflow) settings(pluginSettings: _*)


/* Hook */

def allHook = Seq(displayHook, fileHook, modifierHook)

lazy val displayHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.display", imports = Seq("*")) dependsOn (openmoleDSL) settings(pluginSettings: _*)

lazy val fileHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.file", imports = Seq("*")) dependsOn (openmoleDSL, replication % "test") settings (
  libraryDependencies += Libraries.scalatest
) settings(pluginSettings: _*)

lazy val modifierHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.modifier", imports = Seq("*")) dependsOn (openmoleDSL) settings (
  libraryDependencies += Libraries.scalatest
) settings(pluginSettings: _*)


/* Method */

def allMethod = Seq(evolution, stochastic, abc)

lazy val evolution = OsgiProject(pluginDir, "org.openmole.plugin.method.evolution", imports = Seq("*")) dependsOn (
  openmoleDSL, fileHook, toolsTask, pattern, collectionDomain % "test"
) settings (libraryDependencies += Libraries.mgo) settings(pluginSettings: _*)

lazy val stochastic = OsgiProject(pluginDir, "org.openmole.plugin.method.stochastic", imports = Seq("*")) dependsOn (openmoleDSL, distributionDomain, pattern) settings(pluginSettings: _*)

lazy val abc = OsgiProject(pluginDir, "org.openmole.plugin.method.abc", imports = Seq("*")) dependsOn (openmoleDSL, fileHook, tools) settings
  (libraryDependencies += Libraries.scalabc) settings(pluginSettings: _*)


/* Sampling */

def allSampling = Seq(combineSampling, csvSampling, lhsSampling, quasirandomSampling)

lazy val combineSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.combine", imports = Seq("*")) dependsOn (exception, modifierDomain, collectionDomain, workflow) settings(pluginSettings: _*)

lazy val csvSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.csv", imports = Seq("*")) dependsOn (exception, workflow, csv) settings (
  libraryDependencies += Libraries.scalatest
) settings(pluginSettings: _*)

lazy val lhsSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.lhs", imports = Seq("*")) dependsOn (exception, workflow, workspace) settings(pluginSettings: _*)

lazy val quasirandomSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.quasirandom", imports = Seq("*")) dependsOn (exception, workflow, workspace) settings (
  libraryDependencies += Libraries.math
) settings(pluginSettings: _*)


/* Source */

def allSource = Seq(fileSource)

lazy val fileSource = OsgiProject(pluginDir, "org.openmole.plugin.source.file", imports = Seq("*")) dependsOn (openmoleDSL, serializer, exception, csv) settings(pluginSettings: _*)


/* Task */

def allTask = Seq(toolsTask, external, netLogo, netLogo5, jvm, scala, template, systemexec, care)

lazy val toolsTask = OsgiProject(pluginDir, "org.openmole.plugin.task.tools", imports = Seq("*")) dependsOn (openmoleDSL) settings(pluginSettings: _*)


lazy val external = OsgiProject(pluginDir, "org.openmole.plugin.task.external", imports = Seq("*")) dependsOn (openmoleDSL, workspace) settings(pluginSettings: _*)

lazy val netLogo = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo", imports = Seq("*")) dependsOn (openmoleDSL, external, netLogoAPI) settings(pluginSettings: _*)

lazy val netLogo5 = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo5") dependsOn (netLogo, openmoleDSL, external, netLogo5API) settings(pluginSettings: _*)

lazy val jvm = OsgiProject(pluginDir, "org.openmole.plugin.task.jvm", imports = Seq("*")) dependsOn (openmoleDSL, external, workspace) settings(pluginSettings: _*)

lazy val scala = OsgiProject(pluginDir, "org.openmole.plugin.task.scala", imports = Seq("*")) dependsOn (openmoleDSL, jvm, console) settings(pluginSettings: _*)

lazy val template = OsgiProject(pluginDir, "org.openmole.plugin.task.template", imports = Seq("*")) dependsOn (openmoleDSL, replication % "test") settings (
  libraryDependencies += Libraries.scalatest
) settings(pluginSettings: _*)

lazy val systemexec = OsgiProject(pluginDir, "org.openmole.plugin.task.systemexec", imports = Seq("*")) dependsOn (openmoleDSL, external, workspace) settings (
  libraryDependencies += Libraries.exec) settings(pluginSettings: _*)

lazy val care = OsgiProject(pluginDir, "org.openmole.plugin.task.care", imports = Seq("*")) dependsOn (systemexec) settings (
  libraryDependencies += Libraries.scalatest) settings(pluginSettings: _*)



/* ---------------- REST ------------------- */


def restDir = file("rest")

lazy val message = OsgiProject(restDir, "org.openmole.rest.message") settings (defaultSettings: _*)

lazy val server = OsgiProject(
  restDir,
  "org.openmole.rest.server",
  imports = Seq("org.h2", "!com.sun.*", "*")
) dependsOn (workflow, openmoleTar, openmoleCollection, project, message, openmoleCrypto) settings (
  libraryDependencies ++= Seq(Libraries.bouncyCastle, Libraries.logback, Libraries.scalatra, Libraries.scalaLang, Libraries.arm, Libraries.codec, Libraries.json4s)) settings (defaultSettings: _*)

lazy val client = Project("org-openmole-rest-client", restDir / "client") settings (
  libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.5",
  libraryDependencies += "org.apache.httpcomponents" % "httpmime" % "4.3.5",
  libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.4.0"
) dependsOn (message, openmoleTar) settings (defaultSettings: _*)




/* -------------------- GUI --------------------- */


def guiDir = file("gui")
def guiMiscDir = guiDir / "misc"

lazy val utilsGUI = OsgiProject(guiMiscDir, "org.openmole.gui.misc.utils") enablePlugins (ScalaJSPlugin) settings (
  Libraries.scalaTagsJS,
  Libraries.scaladgetJS,
  libraryDependencies += Libraries.rx) settings (defaultSettings: _*)

lazy val jsGUI = OsgiProject(guiMiscDir, "org.openmole.gui.misc.js") enablePlugins (ScalaJSPlugin) dependsOn(workspace, utilsGUI) settings (
  Libraries.scalajsDomJS,
  Libraries.scalaTagsJS,
  Libraries.scaladgetJS,
  Libraries.rxJS) settings (defaultSettings: _*)

lazy val docGUI = OsgiProject(guiMiscDir, "org.openmole.doc") enablePlugins (ScalaJSPlugin) settings (
  Libraries.scalajsDomJS,
  Libraries.scalaTagsJS,
  Libraries.scaladgetJS,
  Libraries.rxJS,
  libraryDependencies += Libraries.scalaTags) settings (defaultSettings: _*)


def guiExt = guiDir / "ext"

lazy val dataGUI = OsgiProject(guiExt, "org.openmole.gui.ext.data") enablePlugins (ScalaJSPlugin) dependsOn (workflow) settings (
  Libraries.upickleJS) settings (defaultSettings: _*)

lazy val datauiGUI: Project = OsgiProject(guiExt, "org.openmole.gui.ext.dataui") dependsOn (dataGUI, jsGUI) enablePlugins (ScalaJSPlugin) settings (
  Libraries.rxJS,
  Libraries.scalaTagsJS,
  Libraries.scalajsDomJS) settings (defaultSettings: _*)

lazy val sharedGUI = OsgiProject(guiDir / "shared", "org.openmole.gui.shared") dependsOn (dataGUI, buildinfo) settings (defaultSettings: _*)

val jqueryPath = s"META-INF/resources/webjars/jquery/${Libraries.jqueryVersion}/jquery.js"
val acePath = s"META-INF/resources/webjars/ace/${Libraries.aceVersion}/src-min/ace.js"

lazy val clientGUI = OsgiProject(guiDir / "client", "org.openmole.gui.client.core") enablePlugins (ScalaJSPlugin) dependsOn
  (datauiGUI, sharedGUI, utilsGUI, jsGUI, docGUI) settings (
    Libraries.upickleJS,
    Libraries.autowireJS,
    Libraries.rxJS,
    Libraries.scalajsDomJS,
    Libraries.scaladgetJS,
    Libraries.scalaTagsJS,
    libraryDependencies += Libraries.async,
    skip in packageJSDependencies := false,
    jsDependencies += Libraries.jquery / jqueryPath minified jqueryPath.replace(".js", ".min.js"),
    jsDependencies += Libraries.ace / acePath,
    jsDependencies += Libraries.ace / "src-min/mode-sh.js" dependsOn acePath,
    jsDependencies += Libraries.ace / "src-min/mode-scala.js" dependsOn acePath,
    jsDependencies += Libraries.ace / "src-min/theme-github.js" dependsOn acePath,
    jsDependencies += Libraries.bootstrap / "js/bootstrap.js" dependsOn jqueryPath minified "js/bootstrap.min.js"
  ) settings (defaultSettings: _*)


def guiServerDir = guiDir / "server"

lazy val serverGUI = OsgiProject(guiServerDir, "org.openmole.gui.server.core") settings
  (libraryDependencies ++= Seq(Libraries.autowire, Libraries.upickle, Libraries.scalaTags, Libraries.logback, Libraries.scalatra, Libraries.clapper)) dependsOn (
    sharedGUI,
    datauiGUI,
    dataGUI,
    workflow,
    buildinfo,
    openmoleFile,
    openmoleTar,
    openmoleCollection,
    project,
    openmoleDSL,
    batch,
    egi,
    ssh,
    utilsGUI,
    openmoleStream,
    txtmark,
    openmoleCrypto,
    module
  )settings (defaultSettings: _*)

lazy val state = OsgiProject(guiServerDir, "org.openmole.gui.server.state") settings
  (libraryDependencies += Libraries.slick) dependsOn (dataGUI, workflow, workspace) settings (defaultSettings: _*)



/* -------------------- Bin ------------------------- */

def binDir = file("bin")


def bundleFilter(m: ModuleID, artifact: Artifact) = {
  def exclude =
    (m.organization != "org.openmole" && m.name.contains("slick")) ||
      (m.name contains "sshj")

  def include = (artifact.`type` == "bundle" && m.name != "osgi") ||
    m.organization == "org.bouncycastle" ||
    (m.name == "httpclient-osgi") || (m.name == "httpcore-osgi") ||
    (m.organization == "org.osgi" && m.name != "osgi")

  include && !exclude
}

def rename(m: ModuleID): String =
  if (m.name.exists(_ == '-') == false) s"${m.organization.replaceAllLiterally(".", "-")}-${m.name}_${m.revision}.jar"
  else s"${m.name}_${m.revision}.jar"


import Assembly._

lazy val openmoleUI = OsgiProject(binDir,"org.openmole.ui", singleton = true, imports = Seq("*")) settings (
  organization := "org.openmole.ui"
) dependsOn (
    console,
    workspace,
    replication,
    exception,
    tools,
    event,
    pluginManager,
    workflow,
    serverGUI,
    clientGUI,
    logging,
    server,
    consoleBin,
    openmoleDSL
  ) settings (defaultSettings: _*)

def openmoleNakedDependencies = allCore ++ Seq(openmoleUI)
def openmoleDependencies = openmoleNakedDependencies ++ allPlugin

lazy val openmoleNaked =
  Project("openmole-naked", binDir / "openmole-naked", settings = tarProject ++ assemblySettings) settings (
    setExecutable ++= Seq("openmole", "openmole.bat"),
    Osgi.bundleDependencies in Compile := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject, includeRoot = false))).value,
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath).identityMap,
    resourcesAssemble <+= (resourceDirectory in serverGUI in Compile, assemblyPath) map { case (r, p) ⇒ (r / "webapp") → (p / "webapp") },
    resourcesAssemble <+= (fullOptJS in clientGUI in Compile, assemblyPath) map { case (js, p) ⇒ js.data → (p / "webapp/js/openmole.js") },
    resourcesAssemble <+= (packageMinifiedJSDependencies in clientGUI in Compile, assemblyPath) map { case (js, p) ⇒ js → (p / "webapp/js/deps.js") },
    resourcesAssemble <+= (assemble in dbServer, assemblyPath) map { case (r, p) ⇒ r → (p / "dbserver") },
    resourcesAssemble <+= (Tar.tar in openmoleRuntime, assemblyPath) map { case (r, p) ⇒ r → (p / "runtime" / r.getName) },
    resourcesAssemble <+= (assemble in launcher, assemblyPath) map { case (r, p) ⇒ r → (p / "launcher") },
    resourcesAssemble <++= (Osgi.bundleDependencies in Compile, assemblyPath) map { case (bs, a) ⇒ bs.map(b ⇒ b → (a / "plugins" / b.getName)) },
    libraryDependencies += Libraries.logging,
    dependencyFilter := bundleFilter,
    dependencyName := rename,
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    Tar.name := "openmole-naked.tar.gz",
    Tar.innerFolder := "openmole",
    cleanFiles <++= cleanFiles in dbServer,
    cleanFiles <++= cleanFiles in launcher,
    cleanFiles <++= cleanFiles in openmoleRuntime
  ) dependsOn (toDependencies(openmoleNakedDependencies): _*) settings (defaultSettings: _*)

lazy val openmole =
  Project("openmole", binDir / "openmole", settings = tarProject ++ assemblySettings) settings (defaultSettings: _*) settings (
    setExecutable ++= Seq("openmole", "openmole.bat"),
    Osgi.bundleDependencies in Compile := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject, includeRoot = false))).value,
    Tar.name := "openmole.tar.gz",
    Tar.innerFolder := "openmole",
    dependencyFilter := bundleFilter,
    dependencyName := rename,
    resourcesAssemble <+= (assemble in openmoleNaked, assemblyPath).identityMap,
    resourcesAssemble <++= (Osgi.bundleDependencies in Compile, assemblyPath) map { case (bs, a) ⇒ bs.map(b ⇒ b → (a / "plugins" / b.getName)) },
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    cleanFiles <++= cleanFiles in openmoleNaked
  ) dependsOn (toDependencies(openmoleDependencies): _*)

lazy val dbServer = OsgiProject(binDir, "org.openmole.dbserver", settings = assemblySettings) dependsOn (replication) settings (
  assemblyDependenciesPath := assemblyPath.value / "lib",
  resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → (p / "bin") },
  resourcesAssemble <++= (Osgi.bundleDependencies in Compile, assemblyPath) map { case (bs, a) ⇒ bs.map(b ⇒ b → (a / "lib" / b.getName)) },
  libraryDependencies ++= Seq(
    Libraries.xstream,
    Libraries.slick,
    Libraries.h2,
    Libraries.slf4j,
    Libraries.scalaLang
  ),
  dependencyFilter := bundleFilter,
  dependencyName := rename
) settings (defaultSettings: _*)

lazy val openmoleRuntime =
  OsgiProject(binDir, "org.openmole.runtime", singleton = true, imports = Seq("*"), settings = tarProject ++ assemblySettings) dependsOn (workflow, communication, serializer, logging, event, exception) settings (
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → p },
    resourcesAssemble <+= (assemble in launcher, assemblyPath) map { case (r, p) ⇒ r → (p / "launcher") },
    resourcesAssemble <++= (Osgi.bundleDependencies in Compile, assemblyPath) map { case (bs, a) ⇒ bs.map(b ⇒ b → (a / "plugins" / b.getName)) },
    setExecutable ++= Seq("run.sh"),
    Tar.name := "runtime.tar.gz",
    libraryDependencies += Libraries.scopt,
    libraryDependencies += Libraries.logging,
    dependencyFilter := bundleFilter,
    dependencyName := rename
  ) dependsOn (toDependencies(allCore): _*) settings (defaultSettings: _*)


lazy val daemon = OsgiProject(binDir, "org.openmole.daemon", settings = tarProject ++ assemblySettings) dependsOn (workflow, workflow, communication, workspace,
  fileService, exception, tools, logging, desktopgrid) settings (
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    resourcesAssemble <++= (Osgi.bundleDependencies in Compile, assemblyPath) map { case (bs, a) ⇒ bs.map(b ⇒ b → (a / "plugins" / b.getName)) },
    resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → p },
    resourcesAssemble <+= (assemble in launcher, assemblyPath) map { case (r, p) ⇒ r → (p / "launcher") },
    libraryDependencies ++= Seq(
      Libraries.sshd,
      Libraries.gridscale,
      Libraries.gridscaleSSH,
      Libraries.bouncyCastle,
      Libraries.scalaLang,
      Libraries.logging,
      Libraries.scopt
    ),
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    dependencyFilter := bundleFilter,
    dependencyName := rename,
    setExecutable ++= Seq("openmole-daemon", "openmole-daemon.bat"),
    Tar.name := "openmole-daemon.tar.gz",
    Tar.innerFolder := "openmole-daemon"
  ) settings (defaultSettings: _*)


lazy val api = Project("api", binDir / "target" / "api") settings (defaultSettings: _*) settings (
  unidocSettings: _*
) settings (tarProject: _*) settings (
    compile := sbt.inc.Analysis.Empty,
    UnidocKeys.unidocProjectFilter in (ScalaUnidoc, UnidocKeys.unidoc) :=
      inProjects(openmoleDependencies.map(p ⇒ p: ProjectReference): _*), /*-- inProjects(Libraries.projects.map(p ⇒ p: ProjectReference) ++ ThirdParties.projects.map(p ⇒ p: ProjectReference)*/
    Tar.name := "openmole-api.tar.gz",
    Tar.folder <<= (UnidocKeys.unidoc in Compile).map(_.head)
  )

lazy val site =
  OsgiProject(
    binDir,
    "org.openmole.site",
    singleton = true,
    imports = Seq("*"),
    settings = defaultSettings ++ scalatex.SbtPlugin.projectSettings ++ assemblySettings
  ) settings (
      organization := "org.openmole.site",
      OsgiKeys.exportPackage := Seq("scalatex.openmole.*") ++ OsgiKeys.exportPackage.value,
      libraryDependencies += Libraries.scalaLang,
      libraryDependencies += Libraries.xstream,
      libraryDependencies += Libraries.scalatexSite,
      libraryDependencies += Libraries.scalaTags,
      libraryDependencies += Libraries.upickle,
      libraryDependencies += Libraries.spray,
      libraryDependencies += Libraries.lang3,
      libraryDependencies += Libraries.toolxitBibtex intransitive (),
      libraryDependencies += Libraries.json4s,
      libraryDependencies += Libraries.logging,
      setExecutable ++= Seq("site"),
      assemblyDependenciesPath := assemblyPath.value / "plugins",
      resourcesAssemble <++= (Osgi.bundleDependencies in Compile, assemblyPath) map { case (bs, a) ⇒ bs.map(b ⇒ b → (a / "plugins" / b.getName)) },
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ (r / "site") → (p / "site") },
      resourcesAssemble <+= (resourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ r → (p / "resources") },
      resourcesAssemble <+= (sourceDirectory in Compile, assemblyPath) map { case (r, p) ⇒ (r / "md") → (p / "resources" / "md") },
      resourcesAssemble <+= (OsgiKeys.bundle, assemblyPath) map { case (r, p) ⇒ r → (p / "plugins" / r.getName) },
      resourcesAssemble <+= (assemble in launcher, assemblyPath) map { case (r, p) ⇒ r → (p / "launcher") },
      resourcesAssemble <+= (Tar.tar in openmole, assemblyPath) map { case (f, d) ⇒ f → (d / "resources" / f.getName) },
      resourcesAssemble <+= (Tar.tar in daemon, assemblyPath) map { case (f, d) ⇒ f → (d / "resources" / f.getName) },
      resourcesAssemble <+= (Tar.tar in api, assemblyPath) map { case (doc, d) ⇒ doc → (d / "resources" / doc.getName) },
      resourcesAssemble <+= (fullOptJS in siteJS in Compile, assemblyPath) map { case (js, d) ⇒ js.data → (d / "resources" / "sitejs.js") },
      dependencyFilter := bundleFilter,
      dependencyName := rename,
      cleanFiles <++= cleanFiles in openmole
    ) dependsOn (txtmark) dependsOn (toDependencies(openmoleNakedDependencies): _*) dependsOn (toDependencies(openmoleDependencies): _*)



lazy val siteJS = OsgiProject(binDir, "org.openmole.sitejs") settings (
  Libraries.scalaTagsJS,
  Libraries.rxJS,
  Libraries.scaladgetJS
) enablePlugins (ScalaJSPlugin) settings (defaultSettings: _*)


lazy val launcher = OsgiProject(binDir, "org.openmole.launcher", imports = Seq("*"), settings = assemblySettings) settings (
  autoScalaLibrary := false,
  libraryDependencies += Libraries.equinoxOSGi,
  resourcesAssemble <+= (OsgiKeys.bundle, assemblyPath) map { case (f, d) ⇒ f → (d / f.getName) }
) settings (defaultSettings: _*)


lazy val consoleBin = OsgiProject(binDir, "org.openmole.console", imports = Seq("*")) settings (
  libraryDependencies += Libraries.upickle
) dependsOn (
    workflow,
    console,
    project,
    openmoleDSL,
    buildinfo,
    module
  ) settings (defaultSettings: _*)
