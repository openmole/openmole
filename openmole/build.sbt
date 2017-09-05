import org.openmole.buildsystem._
import OMKeys._

import sbt._
import Keys._

import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

organization := "org.openmole"
name := "openmole-root"

def macroParadise =
  addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)

lazy val scalaVersionValue = "2.11.8"

def defaultSettings = BuildSystem.settings ++
  Seq(
    organization := "org.openmole",
    scalaOrganization := "org.typelevel",
    updateOptions := updateOptions.value.withCachedResolution(true),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("staging"),
    resolvers += Resolver.bintrayRepo("projectseptemberinc", "maven"), // For freek
    scalaVersion in Global := scalaVersionValue,
    scalacOptions ++= Seq("-target:jvm-1.8", "-language:higherKinds"),
    scalacOptions += "-Ypartial-unification",
    scalacOptions ++= Seq("-Xmax-classfile-name", "140"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    publishArtifact in (packageDoc in install) := false,
    publishArtifact in (packageSrc in install) := false,
    addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.fullMapped(_ ⇒ scalaVersionValue)),
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  )


publishTo in ThisBuild :=
  (if (isSnapshot.value) Some("OpenMOLE Nexus" at "https://maven.openmole.org/snapshots") else Some("OpenMOLE Nexus" at "https://maven.openmole.org/releases"))


/* ------ Third parties ---------- */

def thirdPartiesDir = file("third-parties")
def thirdPartiesSettings = defaultSettings ++ Seq(libraryDependencies += Libraries.scalatest)
def allThirdParties = Seq(
  openmoleCache,
  openmoleTar,
  openmoleFile,
  openmoleLock,
  openmoleLogger,
  openmoleThread,
  openmoleHash,
  openmoleStream,
  openmoleCollection,
  openmoleCrypto,
  openmoleStatistics,
  openmoleTypes,
  openmoleByteCode,
  openmoleOSGi,
  openmoleRandom,
  openmoleNetwork,
  txtmark)

lazy val openmoleCache = OsgiProject(thirdPartiesDir, "org.openmole.tool.cache", imports = Seq("*")) dependsOn (openmoleLogger) settings (thirdPartiesSettings: _*)
lazy val openmoleTar = OsgiProject(thirdPartiesDir, "org.openmole.tool.tar", imports = Seq("*")) dependsOn (openmoleFile) settings (thirdPartiesSettings: _*)
lazy val openmoleFile = OsgiProject(thirdPartiesDir, "org.openmole.tool.file", imports = Seq("*")) dependsOn(openmoleLock, openmoleStream, openmoleLogger) settings (thirdPartiesSettings: _*)
lazy val openmoleLock = OsgiProject(thirdPartiesDir, "org.openmole.tool.lock", imports = Seq("*")) settings (thirdPartiesSettings: _*)
lazy val openmoleLogger = OsgiProject(thirdPartiesDir, "org.openmole.tool.logger", imports = Seq("*")) settings (thirdPartiesSettings: _*)
lazy val openmoleThread = OsgiProject(thirdPartiesDir, "org.openmole.tool.thread", imports = Seq("*")) dependsOn(openmoleLogger, openmoleCollection) settings (thirdPartiesSettings: _*) settings (libraryDependencies += Libraries.squants)
lazy val openmoleHash = OsgiProject(thirdPartiesDir, "org.openmole.tool.hash", imports = Seq("*")) dependsOn(openmoleFile, openmoleStream) settings (thirdPartiesSettings: _*)
lazy val openmoleStream = OsgiProject(thirdPartiesDir, "org.openmole.tool.stream", imports = Seq("*")) dependsOn (openmoleThread) settings(libraryDependencies += Libraries.collections, libraryDependencies += Libraries.squants) settings (thirdPartiesSettings: _*)
lazy val openmoleCollection = OsgiProject(thirdPartiesDir, "org.openmole.tool.collection", imports = Seq("*")) settings (libraryDependencies += Libraries.scalaLang) settings (thirdPartiesSettings: _*)
lazy val openmoleCrypto = OsgiProject(thirdPartiesDir, "org.openmole.tool.crypto", imports = Seq("*")) settings(libraryDependencies += Libraries.bouncyCastle, libraryDependencies += Libraries.jasypt) settings (thirdPartiesSettings: _*)
lazy val openmoleStatistics = OsgiProject(thirdPartiesDir, "org.openmole.tool.statistics", imports = Seq("*")) dependsOn (openmoleLogger) settings (thirdPartiesSettings: _*)
lazy val openmoleTypes = OsgiProject(thirdPartiesDir, "org.openmole.tool.types", imports = Seq("*")) settings(libraryDependencies += Libraries.shapeless, libraryDependencies += Libraries.squants) settings (thirdPartiesSettings: _*)
lazy val openmoleByteCode = OsgiProject(thirdPartiesDir, "org.openmole.tool.bytecode", imports = Seq("*")) settings (libraryDependencies += Libraries.asm) settings (thirdPartiesSettings: _*)
lazy val openmoleOSGi = OsgiProject(thirdPartiesDir, "org.openmole.tool.osgi", imports = Seq("*")) dependsOn (openmoleFile) settings (libraryDependencies += Libraries.equinoxOSGi) settings (thirdPartiesSettings: _*)
lazy val openmoleRandom = OsgiProject(thirdPartiesDir, "org.openmole.tool.random", imports = Seq("*")) settings (thirdPartiesSettings: _*) settings(libraryDependencies += Libraries.math, libraryDependencies += Libraries.scalaLang) dependsOn (openmoleCache)
lazy val openmoleNetwork = OsgiProject(thirdPartiesDir, "org.openmole.tool.network", imports = Seq("*")) settings (thirdPartiesSettings: _*)

lazy val txtmark = OsgiProject(thirdPartiesDir, "com.quandora.txtmark", exports = Seq("com.github.rjeschke.txtmark.*"), imports = Seq("*")) settings (thirdPartiesSettings: _*)


/* ------------- Core ----------- */

def coreDir = file("core")
def coreProvidedScope = Osgi.openMOLEScope += "provided"
def coreSettings =
  defaultSettings ++
    osgiSettings ++
    Seq(
      coreProvidedScope,
      libraryDependencies += Libraries.scalatest,
      libraryDependencies += Libraries.equinoxOSGi
    )

def allCore = Seq(
  workflow,
  authentication,
  serializer,
  communication,
  openmoleDSL,
  exception,
  tools,
  event,
  replication,
  workspace,
  pluginManager,
  fileService,
  logging,
  output,
  console,
  project,
  buildinfo,
  module,
  market,
  context,
  expansion,
  preference,
  db,
  threadProvider,
  services,
  location)


lazy val context = OsgiProject(coreDir, "org.openmole.core.context", imports = Seq("*")) settings(
  libraryDependencies ++= Seq(Libraries.cats, Libraries.sourceCode), defaultActivator
) dependsOn(tools, workspace, preference) settings (coreSettings: _*)

lazy val expansion = OsgiProject(coreDir, "org.openmole.core.expansion", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.cats)
  ) dependsOn(context, tools, openmoleRandom, openmoleFile, pluginManager, console) settings (coreSettings: _*)

lazy val workflow = OsgiProject(coreDir, "org.openmole.core.workflow", imports = Seq("*")) settings(
  libraryDependencies ++= Seq(Libraries.scalaLang, Libraries.math, Libraries.cats, Libraries.equinoxOSGi, Libraries.shapeless),
  defaultActivator
) dependsOn(
  event,
  exception,
  tools,
  workspace,
  pluginManager,
  serializer,
  output,
  console,
  context,
  preference,
  expansion,
  threadProvider) settings (coreSettings: _*)

lazy val serializer = OsgiProject(coreDir, "org.openmole.core.serializer", global = true, imports = Seq("*")) settings(
  libraryDependencies += Libraries.xstream,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(workspace, pluginManager, fileService, tools, openmoleTar, console) settings (coreSettings: _*)

lazy val communication = OsgiProject(coreDir, "org.openmole.core.communication", imports = Seq("*")) dependsOn(workflow, workspace) settings (coreSettings: _*)

lazy val openmoleDSL = OsgiProject(coreDir, "org.openmole.core.dsl", imports = Seq("*")) settings (
  libraryDependencies += Libraries.squants) dependsOn(workflow, logging) settings (coreSettings: _*)

lazy val exception = OsgiProject(coreDir, "org.openmole.core.exception", imports = Seq("*")) settings (coreSettings: _*)

lazy val tools = OsgiProject(coreDir, "org.openmole.core.tools", global = true, imports = Seq("*")) settings
  (libraryDependencies ++= Seq(Libraries.xstream, Libraries.exec, Libraries.math, Libraries.scalaLang, Libraries.scalatest, Libraries.equinoxOSGi)) dependsOn
  (exception, openmoleTar, openmoleFile, openmoleLock, openmoleThread, openmoleHash, openmoleLogger, openmoleStream, openmoleCollection, openmoleStatistics, openmoleTypes, openmoleCache, openmoleRandom, openmoleNetwork) settings (coreSettings: _*)

lazy val event = OsgiProject(coreDir, "org.openmole.core.event", imports = Seq("*")) dependsOn (tools) settings (coreSettings: _*)

lazy val replication = OsgiProject(coreDir, "org.openmole.core.replication", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.slick, Libraries.xstream, Libraries.guava)) settings (coreSettings: _*) dependsOn(db, preference, workspace, openmoleCache)

lazy val db = OsgiProject(coreDir, "org.openmole.core.db", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.slick, Libraries.xstream, Libraries.h2, Libraries.scopt)) settings (coreSettings: _*) dependsOn(openmoleNetwork, exception, openmoleCrypto, openmoleFile, openmoleLogger)

lazy val preference = OsgiProject(coreDir, "org.openmole.core.preference", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.configuration, Libraries.scalaLang, Libraries.squants)) settings (coreSettings: _*) dependsOn(openmoleNetwork, openmoleCrypto, openmoleFile, openmoleThread, openmoleTypes, openmoleLock, exception)

lazy val workspace = OsgiProject(coreDir, "org.openmole.core.workspace", imports = Seq("*")) dependsOn
  (exception, event, tools, openmoleCrypto) settings (coreSettings: _*)

lazy val authentication = OsgiProject(coreDir, "org.openmole.core.authentication", imports = Seq("*")) dependsOn(workspace, serializer) settings (coreSettings: _*)

lazy val services = OsgiProject(coreDir, "org.openmole.core.services", imports = Seq("*")) dependsOn(workspace, serializer, preference, fileService, threadProvider, replication, authentication) settings (coreSettings: _*)

lazy val location = OsgiProject(coreDir, "org.openmole.core.location", imports = Seq("*")) dependsOn (exception) settings (coreSettings: _*)


lazy val pluginManager = OsgiProject(
  coreDir,
  "org.openmole.core.pluginmanager",
  bundleActivator = Some("org.openmole.core.pluginmanager.internal.Activator"), imports = Seq("*")
) dependsOn(exception, tools, location) settings (coreSettings: _*)

lazy val fileService = OsgiProject(coreDir, "org.openmole.core.fileservice", imports = Seq("*")) dependsOn(tools, workspace, openmoleTar, preference, threadProvider) settings (coreSettings: _*) settings (defaultActivator) settings (
  libraryDependencies += Libraries.guava
  )

lazy val threadProvider = OsgiProject(coreDir, "org.openmole.core.threadprovider", imports = Seq("*")) dependsOn(tools, preference) settings (coreSettings: _*) settings (defaultActivator)

lazy val module = OsgiProject(coreDir, "org.openmole.core.module", imports = Seq("*")) dependsOn(buildinfo, expansion, openmoleHash, openmoleFile, pluginManager) settings (coreSettings: _*) settings(
  libraryDependencies += Libraries.gridscaleHTTP,
  libraryDependencies += Libraries.json4s,
  defaultActivator)

lazy val market = OsgiProject(coreDir, "org.openmole.core.market", imports = Seq("*")) enablePlugins (ScalaJSPlugin) dependsOn(buildinfo, expansion, openmoleHash, openmoleFile, pluginManager) settings (coreSettings: _*) settings(
  libraryDependencies += Libraries.gridscaleHTTP,
  libraryDependencies += Libraries.json4s,
  defaultActivator)

lazy val logging = OsgiProject(
  coreDir,
  "org.openmole.core.logging",
  bundleActivator = Some("org.openmole.core.logging.internal.Activator"), imports = Seq("*")
) settings (libraryDependencies ++= Seq(Libraries.log4j, Libraries.logback, Libraries.slf4j)) dependsOn (tools) settings (coreSettings: _*)

lazy val output = OsgiProject(coreDir, "org.openmole.core.output", imports = Seq("*")) dependsOn (openmoleStream) settings (coreSettings: _*) settings (defaultActivator)

lazy val console = OsgiProject(coreDir, "org.openmole.core.console", global = true, imports = Seq("*")) dependsOn
  (pluginManager) settings(
  OsgiKeys.importPackage := Seq("*"),
  libraryDependencies += Libraries.scalaLang,
  libraryDependencies += Libraries.monocle,
  macroParadise,
  defaultActivator
) dependsOn(openmoleByteCode, openmoleOSGi) settings (coreSettings: _*)

lazy val project = OsgiProject(coreDir, "org.openmole.core.project", imports = Seq("*")) dependsOn(console, openmoleDSL, services) settings (OsgiKeys.importPackage := Seq("*")) settings (coreSettings: _*)

lazy val buildinfo = OsgiProject(coreDir, "org.openmole.core.buildinfo", imports = Seq("*")) settings (buildInfoSettings: _*) settings(
  sourceGenerators in Compile += buildInfo.taskValue,
  buildInfoKeys :=
    Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      }
    ),
  buildInfoPackage := s"org.openmole.core.buildinfo"
) settings (coreSettings: _*)


/* ------------- Plugins ----------- */


def pluginDir = file("plugins")
def corePlugins =
  allTask ++
    allSource ++
    allSampling ++
    allMethod ++
    allHook ++
    allGrouping ++
    allEnvironment ++
    allDomain ++
    allTools

def allTools = Seq(netLogoAPI, netLogo5API, netLogo6API, csv, pattern, sftpserver)

lazy val defaultActivator = OsgiKeys.bundleActivator := Some(name.value + ".Activator")

def pluginSettings =
  defaultSettings ++ Seq(
    defaultActivator,
    libraryDependencies += Libraries.equinoxOSGi,
    libraryDependencies += Libraries.scalatest
  )


/* Tools */

def toolsSettings = defaultSettings ++ Seq(OsgiKeys.bundleActivator := None, libraryDependencies += Libraries.scalatest)



lazy val netLogoAPI = OsgiProject(pluginDir, "org.openmole.plugin.tool.netlogo", imports = Seq("*")) settings(
  autoScalaLibrary := false,
  crossPaths := false
) settings (toolsSettings: _*)


lazy val netLogo5API = OsgiProject(pluginDir, "org.openmole.plugin.tool.netlogo5", imports = Seq("*")) dependsOn (netLogoAPI) settings(
  crossPaths := false,
  autoScalaLibrary := false,
  libraryDependencies += Libraries.netlogo5 intransitive(),
  libraryDependencies -= Libraries.scalatest
) settings (toolsSettings: _*)


lazy val netLogo6API = OsgiProject(pluginDir, "org.openmole.plugin.tool.netlogo6", imports = Seq("*")) dependsOn (netLogoAPI) settings(
  crossPaths := false,
  autoScalaLibrary := false,
  libraryDependencies += Libraries.netlogo6 intransitive(),
  libraryDependencies -= Libraries.scalatest
) settings (toolsSettings: _*)

lazy val csv = OsgiProject(pluginDir, "org.openmole.plugin.tool.csv", imports = Seq("*")) dependsOn(exception, openmoleDSL) settings(
  libraryDependencies += Libraries.opencsv,
  defaultActivator
) settings (toolsSettings: _*)

lazy val pattern = OsgiProject(pluginDir, "org.openmole.plugin.tool.pattern", imports = Seq("*")) dependsOn(exception, openmoleDSL) settings (toolsSettings: _*) settings (defaultActivator)

lazy val sftpserver = OsgiProject(pluginDir, "org.openmole.plugin.tool.sftpserver", imports = Seq("*")) dependsOn (tools) settings (libraryDependencies += Libraries.sshd) settings (toolsSettings: _*)


/* Domain */

def allDomain = Seq(collectionDomain, distributionDomain, fileDomain, modifierDomain, rangeDomain)

lazy val collectionDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.collection", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val distributionDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.distribution", imports = Seq("*")) dependsOn (openmoleDSL) settings
  (libraryDependencies ++= Seq(Libraries.math)) settings (pluginSettings: _*)

lazy val fileDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.file", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val modifierDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.modifier", imports = Seq("*")) dependsOn (openmoleDSL) settings (
  libraryDependencies += Libraries.scalatest
  ) settings (pluginSettings: _*)

lazy val rangeDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.range", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)


/* Environment */

def allEnvironment = Seq(batch, oar, desktopgrid, egi, gridscale, pbs, sge, condor, slurm, ssh)

lazy val batch = OsgiProject(pluginDir, "org.openmole.plugin.environment.batch", imports = Seq("*")) dependsOn(
  workflow, workspace, tools, event, replication, exception,
  serializer, fileService, pluginManager, openmoleTar, communication, authentication, location, services
) settings (
  libraryDependencies ++= Seq(
    Libraries.gridscale,
    Libraries.h2,
    Libraries.guava,
    Libraries.jasypt,
    Libraries.slick
  )
  ) settings (pluginSettings: _*)

lazy val oar = OsgiProject(pluginDir, "org.openmole.plugin.environment.oar", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleOAR) settings (pluginSettings: _*)

lazy val desktopgrid = OsgiProject(pluginDir, "org.openmole.plugin.environment.desktopgrid", imports = Seq("*")) dependsOn(
  openmoleDSL,
  batch, sftpserver, gridscale
) settings (pluginSettings: _*)

lazy val egi = OsgiProject(pluginDir, "org.openmole.plugin.environment.egi", imports = Seq("!org.apache.http.*", "!fr.iscpif.gridscale.libraries.srmstub", "!fr.iscpif.gridscale.libraries.lbstub", "!fr.iscpif.gridscale.libraries.wmsstub", "!com.google.common.cache", "*")) dependsOn(openmoleDSL,
  batch,
  workspace, fileService, gridscale) settings (
  libraryDependencies ++= Seq(Libraries.gridscaleGlite, Libraries.gridscaleHTTP, Libraries.scalaLang)
  ) settings (pluginSettings: _*)

lazy val gridscale = OsgiProject(pluginDir, "org.openmole.plugin.environment.gridscale", imports = Seq("*")) dependsOn(openmoleDSL, tools,
  batch, exception) settings (pluginSettings: _*)

lazy val pbs = OsgiProject(pluginDir, "org.openmole.plugin.environment.pbs", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscalePBS) settings (pluginSettings: _*)

lazy val sge = OsgiProject(pluginDir, "org.openmole.plugin.environment.sge", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleSGE) settings (pluginSettings: _*)

lazy val condor = OsgiProject(pluginDir, "org.openmole.plugin.environment.condor", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleCondor) settings (pluginSettings: _*)

lazy val slurm = OsgiProject(pluginDir, "org.openmole.plugin.environment.slurm", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleSLURM) settings (pluginSettings: _*)

lazy val ssh = OsgiProject(pluginDir, "org.openmole.plugin.environment.ssh", imports = Seq("*")) dependsOn(openmoleDSL, event, batch, gridscale) settings
  (libraryDependencies += Libraries.gridscaleSSH) settings (pluginSettings: _*)


/* Grouping */

def allGrouping = Seq(batchGrouping, onvariableGrouping)

lazy val batchGrouping = OsgiProject(pluginDir, "org.openmole.plugin.grouping.batch", imports = Seq("*")) dependsOn(exception, workflow, workspace) settings (pluginSettings: _*)

lazy val onvariableGrouping = OsgiProject(pluginDir, "org.openmole.plugin.grouping.onvariable", imports = Seq("*")) dependsOn(exception, workflow) settings (pluginSettings: _*)


/* Hook */

def allHook = Seq(displayHook, fileHook, modifierHook)

lazy val displayHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.display", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val fileHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.file", imports = Seq("*")) dependsOn(openmoleDSL, replication % "test") settings (
  libraryDependencies += Libraries.scalatest
  ) settings (pluginSettings: _*)

lazy val modifierHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.modifier", imports = Seq("*")) dependsOn (openmoleDSL) settings (
  libraryDependencies += Libraries.scalatest
  ) settings (pluginSettings: _*)


/* Method */

def allMethod = Seq(evolution, directSampling, abc)

lazy val evolution = OsgiProject(pluginDir, "org.openmole.plugin.method.evolution", imports = Seq("*")) dependsOn(
  openmoleDSL, fileHook, toolsTask, pattern, collectionDomain % "test"
) settings(libraryDependencies += Libraries.mgo, libraryDependencies += Libraries.shapeless) settings (pluginSettings: _*)

lazy val abc = OsgiProject(pluginDir, "org.openmole.plugin.method.abc", imports = Seq("*")) dependsOn(openmoleDSL, fileHook, tools) settings
  (libraryDependencies += Libraries.scalabc) settings (pluginSettings: _*)

lazy val directSampling = OsgiProject(pluginDir, "org.openmole.plugin.method.directsampling", imports = Seq("*")) dependsOn(openmoleDSL, distributionDomain, pattern, modifierDomain) settings (pluginSettings: _*)



/* Sampling */

def allSampling = Seq(combineSampling, csvSampling, lhsSampling, quasirandomSampling)

lazy val combineSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.combine", imports = Seq("*")) dependsOn(exception, modifierDomain, collectionDomain, workflow) settings (pluginSettings: _*)

lazy val csvSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.csv", imports = Seq("*")) dependsOn(exception, workflow, csv) settings (
  libraryDependencies += Libraries.scalatest
  ) settings (pluginSettings: _*)

lazy val lhsSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.lhs", imports = Seq("*")) dependsOn(exception, workflow, workspace) settings (pluginSettings: _*)

lazy val quasirandomSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.quasirandom", imports = Seq("*")) dependsOn(exception, workflow, workspace) settings (
  libraryDependencies += Libraries.math
  ) settings (pluginSettings: _*)


/* Source */

def allSource = Seq(fileSource)

lazy val fileSource = OsgiProject(pluginDir, "org.openmole.plugin.source.file", imports = Seq("*")) dependsOn(openmoleDSL, serializer, exception, csv) settings (pluginSettings: _*)


/* Task */

def allTask = Seq(toolsTask, external, netLogo, netLogo5, netLogo6, jvm, scala, template, systemexec, container, care, udocker)

lazy val toolsTask = OsgiProject(pluginDir, "org.openmole.plugin.task.tools", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val external = OsgiProject(pluginDir, "org.openmole.plugin.task.external", imports = Seq("*")) dependsOn(openmoleDSL, workspace) settings (pluginSettings: _*)

lazy val netLogo = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo", imports = Seq("*")) dependsOn(openmoleDSL, external, netLogoAPI) settings (pluginSettings: _*)

lazy val netLogo5 = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo5") dependsOn(netLogo, openmoleDSL, external, netLogo5API) settings (pluginSettings: _*)

lazy val netLogo6 = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo6") dependsOn(netLogo, openmoleDSL, external, netLogo6API) settings (pluginSettings: _*)

lazy val jvm = OsgiProject(pluginDir, "org.openmole.plugin.task.jvm", imports = Seq("*")) dependsOn(openmoleDSL, external, workspace) settings (pluginSettings: _*)

lazy val scala = OsgiProject(pluginDir, "org.openmole.plugin.task.scala", imports = Seq("*")) dependsOn(openmoleDSL, jvm, console) settings (pluginSettings: _*)

lazy val template = OsgiProject(pluginDir, "org.openmole.plugin.task.template", imports = Seq("*")) dependsOn(openmoleDSL, replication % "test") settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings: _*)

lazy val systemexec = OsgiProject(pluginDir, "org.openmole.plugin.task.systemexec", imports = Seq("*")) dependsOn(openmoleDSL, external, workspace) settings (
  libraryDependencies += Libraries.exec) settings (pluginSettings: _*)

lazy val container = OsgiProject(pluginDir, "org.openmole.plugin.task.container", imports = Seq("*")) dependsOn(openmoleFile, pluginManager, external, expansion, exception) settings (pluginSettings: _*)

lazy val care = OsgiProject(pluginDir, "org.openmole.plugin.task.care", imports = Seq("*")) dependsOn(systemexec, container) settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings: _*)

lazy val udocker = OsgiProject(pluginDir, "org.openmole.plugin.task.udocker", imports = Seq("*")) dependsOn(systemexec, container) settings(
  libraryDependencies += Libraries.scalatest,
  libraryDependencies += Libraries.json4s,
  libraryDependencies += Libraries.httpClient) settings (pluginSettings: _*)

/* ---------------- REST ------------------- */


def restDir = file("rest")

lazy val message = OsgiProject(restDir, "org.openmole.rest.message") settings (defaultSettings: _*)

lazy val server = OsgiProject(
  restDir,
  "org.openmole.rest.server",
  imports = Seq("org.h2", "!com.sun.*", "*")
) dependsOn(workflow, openmoleTar, openmoleCollection, project, message, openmoleCrypto, services) settings (
  libraryDependencies ++= Seq(Libraries.bouncyCastle, Libraries.logback, Libraries.scalatra, Libraries.scalaLang, Libraries.arm, Libraries.codec, Libraries.json4s)) settings (defaultSettings: _*)

lazy val client = Project("org-openmole-rest-client", restDir / "client") settings(
  libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.5",
  libraryDependencies += "org.apache.httpcomponents" % "httpmime" % "4.3.5",
  libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.4.0"
) dependsOn(message, openmoleTar) settings (defaultSettings: _*)



/* -------------------- GUI --------------------- */


def guiDir = file("gui")

def guiExt = guiDir / "ext"
def guiExtTarget = guiExt / "target"


/* -------------------- Ext ----------------------*/

lazy val dataGUI = OsgiProject(guiExt, "org.openmole.gui.ext.data") enablePlugins (ScalaJSPlugin) settings(
  Libraries.scalaTagsJS,
  Libraries.scalajsDomJS,
  libraryDependencies += Libraries.monocle
) settings (defaultSettings: _*)

lazy val extServerTool = OsgiProject(guiExt, "org.openmole.gui.ext.tool.server") dependsOn(dataGUI, workspace) settings(
  libraryDependencies += Libraries.autowire,
  libraryDependencies += Libraries.upickle
) settings (defaultSettings: _*)

lazy val extClientTool = OsgiProject(guiExt, "org.openmole.gui.ext.tool.client") enablePlugins (ScalaJSPlugin) dependsOn(dataGUI, sharedGUI) settings(
  Libraries.upickleJS,
  Libraries.autowireJS,
  Libraries.rxJS,
  Libraries.scalajsDomJS,
  Libraries.scaladgetJS,
  Libraries.scalaTagsJS
) settings (defaultSettings: _*)

lazy val extPluginGUIServer = OsgiProject(guiExt, "org.openmole.gui.ext.plugin.server") dependsOn(extServerTool, services) settings (
  libraryDependencies += Libraries.equinoxOSGi) settings (defaultSettings: _*)

lazy val sharedGUI = OsgiProject(guiExt, "org.openmole.gui.ext.api") dependsOn(dataGUI, market) settings (defaultSettings: _*)

val acePath = s"META-INF/resources/webjars/ace/${Libraries.aceVersion}/src-min/ace.js"

lazy val jsCompile = OsgiProject(guiServerDir, "org.openmole.gui.server.jscompile", imports = Seq("*")) dependsOn(pluginManager, fileService, workspace, dataGUI) settings (defaultSettings: _*) settings(
  libraryDependencies += "org.scala-js" %% "scalajs-library" % Libraries.scalajsVersion % "provided" intransitive(),
  libraryDependencies += Libraries.scalajsTools,
  (resourceDirectories in Compile) += (crossTarget.value / "resources"),
  (OsgiKeys.embeddedJars) := {
    val scalaLib =
      (Keys.externalDependencyClasspath in Compile).value.filter {
        d => d.data.getName startsWith "scalajs-library"
      }.head

    val dest = crossTarget.value / "resources/scalajs-library.jar"
    dest.getParentFile.mkdirs()
    sbt.IO.copyFile(scalaLib.data, dest)
    Seq()
  })


/* -------------- Client ------------------- */

/*val webpackSettings = Seq(
 webpackConfigFile := Some((resourceDirectory in Compile).value / "webpack.config.js"),
  webpackEntries in(Compile, fullOptJS) := {
    val sjsOutput = (fullOptJS in Compile).value.data
    Seq((sjsOutput.name.stripSuffix(".js"), sjsOutput))
  },
  scalaJSLauncher in(Compile, fullOptJS) := {
    Attributed.blank[VirtualJSFile](FileVirtualJSFile((fullOptJS in Compile).value.data))
  }
)*/

def guiClientDir = guiDir / "client"
lazy val clientGUI = OsgiProject(guiClientDir, "org.openmole.gui.client.core") enablePlugins (ScalaJSPlugin) dependsOn
  (sharedGUI, clientToolGUI, market, dataGUI, extClientTool) settings(
  //webpackSettings,
  libraryDependencies += Libraries.async,
  skip in packageJSDependencies := false,
  jsDependencies += ProvidedJS / "openmole_grammar.js",
  jsDependencies += Libraries.ace / acePath,
  jsDependencies += Libraries.ace / "src-min/mode-sh.js" dependsOn acePath,
  jsDependencies += Libraries.ace / "src-min/mode-scala.js" dependsOn acePath,
  jsDependencies += Libraries.ace / "src-min/theme-github.js" dependsOn acePath
) settings (defaultSettings: _*)


lazy val clientToolGUI = OsgiProject(guiClientDir, "org.openmole.gui.client.tool", privatePackages = Seq("autowire.*", "upickle.*", "sourcecode.*", "rx.*", "org.scalajs.dom.*", "scalatags.*", "scaladget.*")) enablePlugins (ScalaJSPlugin) dependsOn (workspace) settings(
  Libraries.autowireJS,
  Libraries.upickleJS,
  Libraries.scalajsDomJS,
  Libraries.scalaTagsJS,
  Libraries.scaladgetJS,
  Libraries.rxJS) dependsOn (extClientTool) settings (defaultSettings: _*)


/* -------------------------- Server ----------------------- */

def guiServerDir = guiDir / "server"

lazy val serverGUI = OsgiProject(guiServerDir, "org.openmole.gui.server.core") settings
  (libraryDependencies ++= Seq(Libraries.autowire, Libraries.upickle, Libraries.scalaTags, Libraries.logback, Libraries.scalatra, Libraries.clapper)) dependsOn(
  sharedGUI,
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
  openmoleStream,
  txtmark,
  openmoleCrypto,
  module,
  market,
  extServerTool,
  extPluginGUIServer,
  jsCompile,
  services,
  location
) settings (defaultSettings: _*)

/* -------------------- GUI Plugin ----------------------- */

def guiPluginSettings = defaultSettings ++ Seq(defaultActivator)

def guiPluginDir = guiDir / "plugins"

lazy val guiEnvironmentEGIPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.egi") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(extPluginGUIServer, extClientTool, dataGUI, workspace, egi) enablePlugins (ScalaJSPlugin)

lazy val guiEnvironmentSSHKeyPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.sshkey") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(extPluginGUIServer, extClientTool, dataGUI, workspace, ssh) enablePlugins (ScalaJSPlugin)

lazy val guiEnvironmentSSHLoginPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.sshlogin") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(extPluginGUIServer, extClientTool, dataGUI, workspace, ssh) enablePlugins (ScalaJSPlugin)

lazy val guiEnvironmentDesktopGridPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.desktopgrid") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(extPluginGUIServer, extClientTool, dataGUI, workspace, desktopgrid) enablePlugins (ScalaJSPlugin)

val guiPlugins = Seq(guiEnvironmentEGIPlugin, guiEnvironmentSSHLoginPlugin, guiEnvironmentSSHKeyPlugin, guiEnvironmentDesktopGridPlugin)

/* -------------------- Bin ------------------------- */

def binDir = file("bin")


def bundleFilter(m: ModuleID, artifact: Artifact) = {
  def exclude =
    (m.organization != "org.openmole.library" && m.name.contains("slick")) || (m.name contains "sshj") || (m.name contains "scala-xml")

  def include = (artifact.`type` == "bundle" && m.name != "osgi") ||
    m.organization == "org.bouncycastle" ||
    (m.name == "httpclient-osgi") || (m.name == "httpcore-osgi") ||
    (m.organization == "org.osgi" && m.name != "osgi")

  include && !exclude
}

def noDependencyFilter(m: ModuleID, artifact: Artifact) = false

def rename(m: ModuleID): String =
  if (m.name.exists(_ == '-') == false) s"${m.organization.replaceAllLiterally(".", "-")}-${m.name}_${m.revision}.jar"
  else s"${m.name}_${m.revision}.jar"


import Assembly._

lazy val openmoleUI = OsgiProject(binDir, "org.openmole.ui", singleton = true, imports = Seq("*")) settings (
  organization := "org.openmole.ui"
  ) dependsOn(
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

def minimumPlugins =
  Seq(
    collectionDomain,
    distributionDomain,
    fileDomain,
    modifierDomain,
    rangeDomain,
    combineSampling,
    scala,
    batchGrouping
  )

def openmoleNakedDependencies = allCore ++ Seq(openmoleUI) ++ minimumPlugins

def openmoleDependencies = openmoleNakedDependencies ++ corePlugins ++ guiPlugins

lazy val openmoleNaked =
  Project("openmole-naked", binDir / "openmole-naked") settings (assemblySettings: _*) settings(
    setExecutable ++= Seq("openmole", "openmole.bat"),
    Osgi.bundleDependencies in Compile := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject, includeRoot = false))).value,
    resourcesAssemble += (resourceDirectory in Compile).value -> assemblyPath.value,
    resourcesAssemble += ((resourceDirectory in serverGUI in Compile).value / "webapp") → (assemblyPath.value / "webapp"),
    //  resourcesAssemble += (webpack in(clientGUI, fullOptJS) in Compile).value.head -> (assemblyPath.value / "webapp/js/openmole.js"),
    resourcesAssemble += (fullOptJS in clientGUI in Compile).value.data -> (assemblyPath.value / "webapp/js/openmole.js"),
    resourcesAssemble += (packageMinifiedJSDependencies in clientGUI in Compile).value -> (assemblyPath.value / "webapp/js/deps.js"),
    resourcesAssemble += {
      val tarFile = (tar in openmoleRuntime).value
      tarFile -> (assemblyPath.value / "runtime" / tarFile.getName)
    },
    resourcesAssemble += (assemble in launcher).value -> (assemblyPath.value / "launcher"),
    resourcesAssemble ++= (Osgi.bundleDependencies in Compile).value.map(b ⇒ b → (assemblyPath.value / "plugins" / b.getName)),
    libraryDependencies += Libraries.logging,
    dependencyFilter := bundleFilter,
    dependencyName := rename,
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    tarName := "openmole-naked.tar.gz",
    tarInnerFolder := "openmole",
    cleanFiles ++= (cleanFiles in launcher).value,
    cleanFiles ++= (cleanFiles in openmoleRuntime).value) dependsOn (toDependencies(openmoleNakedDependencies): _*) settings (defaultSettings: _*)

lazy val openmole =
  Project("openmole", binDir / "openmole") enablePlugins (TarPlugin) settings (assemblySettings: _*) settings (defaultSettings: _*) settings(
    setExecutable ++= Seq("openmole", "openmole.bat"),
    Osgi.bundleDependencies in Compile := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject, includeRoot = false))).value,
    tarName := "openmole.tar.gz",
    tarInnerFolder := "openmole",
    dependencyFilter := bundleFilter,
    dependencyName := rename,
    resourcesAssemble += (assemble in openmoleNaked).value -> assemblyPath.value,
    resourcesAssemble ++= (Osgi.bundleDependencies in Compile).value.map(b ⇒ b → (assemblyPath.value / "plugins" / b.getName)),
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    cleanFiles ++= (cleanFiles in openmoleNaked).value) dependsOn (toDependencies(openmoleDependencies): _*)

lazy val openmoleRuntime =
  OsgiProject(binDir, "org.openmole.runtime", singleton = true, imports = Seq("*")) enablePlugins (TarPlugin) settings (assemblySettings: _*) dependsOn(workflow, communication, serializer, logging, event, exception) settings(
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    resourcesAssemble += (resourceDirectory in Compile).value -> assemblyPath.value,
    resourcesAssemble += (assemble in launcher).value -> (assemblyPath.value / "launcher"),
    resourcesAssemble ++= (Osgi.bundleDependencies in Compile).value.map(b ⇒ b → (assemblyPath.value / "plugins" / b.getName)),
    setExecutable ++= Seq("run.sh"),
    tarName := "runtime.tar.gz",
    libraryDependencies += Libraries.scopt,
    libraryDependencies += Libraries.logging,
    dependencyFilter := bundleFilter,
    dependencyName := rename
  ) dependsOn (toDependencies(allCore): _*) settings (defaultSettings: _*)


lazy val daemon = OsgiProject(binDir, "org.openmole.daemon") enablePlugins (TarPlugin) settings (assemblySettings: _*) dependsOn(workflow, workflow, communication, workspace,
  fileService, exception, tools, logging, desktopgrid) settings(
  assemblyDependenciesPath := assemblyPath.value / "plugins",
  resourcesAssemble ++= (Osgi.bundleDependencies in Compile).value.map(b ⇒ b → (assemblyPath.value / "plugins" / b.getName)),
  resourcesAssemble += (resourceDirectory in Compile).value -> assemblyPath.value,
  resourcesAssemble += (assemble in launcher).value -> (assemblyPath.value / "launcher"),
  libraryDependencies ++= Seq(
    Libraries.sshd,
    Libraries.gridscale,
    Libraries.gridscaleSSH,
    Libraries.bouncyCastle,
    Libraries.scalaLang,
    Libraries.logging,
    Libraries.scopt
  ),
  defaultActivator,
  assemblyDependenciesPath := assemblyPath.value / "plugins",
  dependencyFilter := bundleFilter,
  dependencyName := rename,
  setExecutable ++= Seq("openmole-daemon", "openmole-daemon.bat"),
  tarName := "openmole-daemon.tar.gz",
  tarInnerFolder := "openmole-daemon"
) settings (defaultSettings: _*)


lazy val api = Project("api", binDir / "target" / "api") settings (defaultSettings: _*) enablePlugins (ScalaUnidocPlugin) settings(
  compile := sbt.inc.Analysis.Empty,
  unidocProjectFilter in(ScalaUnidoc, unidoc) := inProjects(openmoleDependencies.map(p ⇒ p: ProjectReference): _*)
  // -- inProjects(Libraries.projects.map(p ⇒ p: ProjectReference) ++ ThirdParties.projects.map(p ⇒ p: ProjectReference)*/
  //  Tar.name := "openmole-api.tar.gz",
  //  Tar.folder := (UnidocKeys.unidoc in Compile).map(_.head).value
)


lazy val site = crossProject.in(binDir / "org.openmole.site") settings (defaultSettings: _*) jvmSettings (scalatex.SbtPlugin.projectSettings) jvmSettings(
  libraryDependencies += Libraries.scalaz,
  libraryDependencies += Libraries.scalatexSite,
  libraryDependencies += Libraries.json4s,
  libraryDependencies += Libraries.spray,
  libraryDependencies += Libraries.txtmark,
  libraryDependencies += Libraries.scalaTags
) jsSettings(
  Libraries.rxJS,
  Libraries.scaladgetJS,
  Libraries.scalajsDomJS,
  Libraries.scalajsMarked
)

lazy val siteJS = site.js
lazy val siteJVM = site.jvm dependsOn(tools, project, serializer, marketIndex) settings (
  libraryDependencies += Libraries.sourceCode
  ) dependsOn (marketIndex)

lazy val marketIndex = Project("marketindex", binDir / "org.openmole.marketindex") settings (defaultSettings: _*) settings (
  libraryDependencies += Libraries.json4s
  ) dependsOn(buildinfo, openmoleFile, openmoleTar, market)

def parse(key: String, default: sbt.File, parsed: Seq[String]) = parsed.indexOf(key) match {
  case -1 => (default, parsed ++ Seq(key, default.getAbsolutePath))
  case i: Int => {
    if (i == parsed.size - 1) (default, parsed :+ default.getAbsolutePath)
    else (file(parsed(i + 1)), parsed)
  }
}

lazy val buildSite = inputKey[File]("buildSite")
buildSite := {
  import sbt.complete.Parsers.spaceDelimited

  val siteTarget = Def.inputTaskDyn {
    val parsed = spaceDelimited("<args>").parsed
    val defaultDest = (target in siteJVM).value / "site"
    val (siteTarget, args) = parse("--target", defaultDest, parsed)

    (run in siteJVM in Compile).toTask(" " + args.mkString(" ")).map(_ => siteTarget)
  }.evaluated

  def copySiteResources(siteBuildJS: File, resourceDirectory: File, siteTarget: File) = {
    IO.copyFile(siteBuildJS, siteTarget / "js/sitejs.js")
    IO.copyDirectory(resourceDirectory / "js", siteTarget / "js")
    IO.copyDirectory(resourceDirectory / "css", siteTarget / "css")
    IO.copyDirectory(resourceDirectory / "fonts", siteTarget / "fonts")
    IO.copyDirectory(resourceDirectory / "img", siteTarget / "img")
    IO.copyDirectory(resourceDirectory / "bibtex", siteTarget / "bibtex")
    IO.copyDirectory(resourceDirectory / "script", siteTarget / "script")
    IO.copyDirectory(resourceDirectory / "paper", siteTarget / "paper")
  }

  copySiteResources((fullOptJS in siteJS in Compile).value.data, (resourceDirectory in siteJVM in Compile).value, siteTarget)

  siteTarget
}


def siteTests = Def.taskDyn {
  val testTarget = (target in siteJVM).value / "tests"
  IO.delete(testTarget)
  (run in siteJVM in Compile).toTask(" --test --target " + testTarget).map(_ => testTarget)
}


lazy val tests = Project("tests", binDir / "tests") settings (defaultSettings: _*) settings (assemblySettings: _*) settings(
  resourcesAssemble += (siteTests.value -> (assemblyPath.value / "tests")),
  dependencyFilter := noDependencyFilter
)

lazy val modules =
  OsgiProject(
    binDir,
    "org.openmole.modules",
    singleton = true,
    imports = Seq("*"),
    settings = defaultSettings ++ assemblySettings
  ) settings(
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    setExecutable ++= Seq("modules"),
    resourcesAssemble += {
      val bundle = OsgiKeys.bundle.value
      bundle -> (assemblyPath.value / "plugins" / bundle.getName)
    },
    resourcesAssemble ++= (Osgi.bundleDependencies in Compile).value.map(b ⇒ b → (assemblyPath.value / "plugins" / b.getName)),
    resourcesAssemble += ((resourceDirectory in Compile).value / "modules") -> (assemblyPath.value / "modules"),
    resourcesAssemble += (assemble in launcher).value -> (assemblyPath.value / "launcher"),
    dependencyFilter := bundleFilter) dependsOn (toDependencies(openmoleNakedDependencies): _*) dependsOn (toDependencies(openmoleDependencies): _*)

lazy val launcher = OsgiProject(binDir, "org.openmole.launcher", imports = Seq("*"), settings = assemblySettings) settings(
  autoScalaLibrary := false,
  libraryDependencies += Libraries.equinoxOSGi,
  resourcesAssemble += {
    val bundle = (OsgiKeys.bundle).value
    bundle -> (assemblyPath.value / bundle.getName)
  }
) settings (defaultSettings: _*)


lazy val consoleBin = OsgiProject(binDir, "org.openmole.console", imports = Seq("*")) settings (
  libraryDependencies += Libraries.upickle
  ) dependsOn(
  workflow,
  console,
  project,
  openmoleDSL,
  buildinfo,
  module
) settings (defaultSettings: _*)



lazy val dockerBin = Project("docker", binDir / "docker") enablePlugins (sbtdocker.DockerPlugin) settings(
  imageNames in docker := Seq(
    ImageName("openmole/openmole:latest"),

    ImageName(
      namespace = Some("openmole"),
      repository = "openmole",
      tag = Some(version.value)
    )
  ),
  dockerfile in docker := new Dockerfile {
    from("openjdk:8-jre")
    maintainer("Romain Reuillon <romain.reuillon@iscpif.fr>, Jonathan Passerat-Palmbach <j.passerat-palmbach@imperial.ac.uk>")
    copy((assemble in openmole).value, s"/openmole")
    runRaw(
      """groupadd -r openmole && \
              useradd -r -g openmole openmole --home-dir /var/openmole/ --create-home && \
              chmod +x /openmole/openmole && \
              ln -s /openmole/openmole /usr/bin/openmole""")
    expose(8443)
    user("openmole")
    cmdShell("/openmole/openmole", "--port", "8443", "--remote")
  }
)
