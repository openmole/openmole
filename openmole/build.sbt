import org.openmole.buildsystem._
import OMKeys._
import sbt.{io, _}
import Keys.{libraryDependencies, _}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import execnpm.NpmDeps.Dep

import _root_.openmole.common._

organization := "org.openmole"
name := "openmole-root"

import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

def formatSettings =
  Seq(
    ScalariformKeys.preferences :=
      ScalariformKeys.preferences(p =>
        p.setPreference(RewriteArrowSymbols, true)
          .setPreference(AlignParameters, true)
          .setPreference(AlignSingleLineCaseStatements, true)
          .setPreference(DanglingCloseParenthesis, Preserve)
          .setPreference(CompactControlReadability, true)
      ).value,
    scalariformAutoformat := true
  )

def defaultSettings = formatSettings ++
  Seq(
    organization := "org.openmole",
    updateOptions := updateOptions.value.withCachedResolution(true),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("staging"),
    resolvers += Resolver.bintrayRepo("projectseptemberinc", "maven"), // For freek
    resolvers += Resolver.bintrayRepo("definitelyscala", "maven"), // For plotlyjs
    Global / scalaVersion := scalaVersionValue, // + "-bin-typelevel-4",
    scalacOptions ++= Seq("-target:11", "-language:higherKinds"),
    //scalacOptions += "-Yinduction-heuristics",
    //scalacOptions ++= Seq("-Xmax-classfile-name", "140"),
    javacOptions ++= Seq("-source", "11", "-target", "11"),
    install / packageDoc / publishArtifact := false,
    install / packageSrc / publishArtifact := false,
    scalacOptions ++= Seq("-Ymacro-annotations", "-language:postfixOps", "-Ydelambdafy:inline"),
    //scalaOrganization := "org.typelevel",
    //scalaVersion := "2.12.4-bin-typelevel-4",
    //addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M10" cross CrossVersion.full),
    //scalacOptions += "-Xplugin-require:macroparadise",
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " },
  )

//Global / concurrentRestrictions := Seq(Tags.limitAll(4))
//fork in Test in ThisBuild := true

ThisBuild / publishTo :=
  (if (isSnapshot.value) Some("OpenMOLE Nexus" at "https://maven.openmole.org/snapshots") else Some("OpenMOLE Nexus" at "https://maven.openmole.org/releases"))


/* ------ Third parties ---------- */

def thirdPartiesDir = file("third-parties")
def thirdPartiesSettings = defaultSettings ++ Seq(libraryDependencies += Libraries.scalatest)
def allThirdParties = Seq(
  openmoleCache,
  openmoleTar,
  openmoleDTW,
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
  openmoleException,
  openmoleOutputRedirection,
  txtmark)

lazy val openmoleCache = OsgiProject(thirdPartiesDir, "org.openmole.tool.cache", imports = Seq("*")) dependsOn (openmoleLogger) settings (thirdPartiesSettings: _*) settings(libraryDependencies += Libraries.squants, libraryDependencies += Libraries.cats)
lazy val openmoleTar = OsgiProject(thirdPartiesDir, "org.openmole.tool.tar", imports = Seq("*")) dependsOn (openmoleFile) settings (thirdPartiesSettings: _*) settings (libraryDependencies += Libraries.xzJava)
lazy val openmoleDTW = OsgiProject(thirdPartiesDir, "org.openmole.tool.dtw", imports = Seq("*")) settings (thirdPartiesSettings: _*)
lazy val openmoleFile = OsgiProject(thirdPartiesDir, "org.openmole.tool.file", imports = Seq("*")) dependsOn(openmoleLock, openmoleStream, openmoleLogger) settings (thirdPartiesSettings: _*)
lazy val openmoleLock = OsgiProject(thirdPartiesDir, "org.openmole.tool.lock", imports = Seq("*")) settings (thirdPartiesSettings: _*)
lazy val openmoleLogger = OsgiProject(thirdPartiesDir, "org.openmole.tool.logger", imports = Seq("*")) dependsOn (openmoleOutputRedirection) settings (thirdPartiesSettings: _*) settings (libraryDependencies += Libraries.sourceCode)
lazy val openmoleThread = OsgiProject(thirdPartiesDir, "org.openmole.tool.thread", imports = Seq("*")) dependsOn(openmoleLogger, openmoleCollection) settings (thirdPartiesSettings: _*) settings (libraryDependencies += Libraries.squants)
lazy val openmoleHash = OsgiProject(thirdPartiesDir, "org.openmole.tool.hash", imports = Seq("*")) dependsOn(openmoleFile, openmoleStream) settings (thirdPartiesSettings: _*)
lazy val openmoleStream = OsgiProject(thirdPartiesDir, "org.openmole.tool.stream", imports = Seq("*")) dependsOn (openmoleThread) settings(libraryDependencies += Libraries.collections, libraryDependencies += Libraries.squants) settings (thirdPartiesSettings: _*)
lazy val openmoleCollection = OsgiProject(thirdPartiesDir, "org.openmole.tool.collection", imports = Seq("*")) settings (Libraries.addScalaLang(scalaVersionValue)) settings (thirdPartiesSettings: _*)
lazy val openmoleCrypto = OsgiProject(thirdPartiesDir, "org.openmole.tool.crypto", imports = Seq("*")) settings(libraryDependencies += Libraries.bouncyCastle, libraryDependencies += Libraries.jasypt) settings (thirdPartiesSettings: _*)
lazy val openmoleStatistics = OsgiProject(thirdPartiesDir, "org.openmole.tool.statistics", imports = Seq("*")) dependsOn(openmoleLogger, openmoleTypes, openmoleDTW) settings (thirdPartiesSettings: _*) settings (libraryDependencies += Libraries.math)
lazy val openmoleTypes = OsgiProject(thirdPartiesDir, "org.openmole.tool.types", imports = Seq("*")) settings(libraryDependencies += Libraries.shapeless, libraryDependencies += Libraries.squants, Libraries.addScalaLang(scalaVersionValue)) settings (thirdPartiesSettings: _*)
lazy val openmoleByteCode = OsgiProject(thirdPartiesDir, "org.openmole.tool.bytecode", imports = Seq("*")) settings (libraryDependencies += Libraries.asm) settings (thirdPartiesSettings: _*)
lazy val openmoleOSGi = OsgiProject(thirdPartiesDir, "org.openmole.tool.osgi", imports = Seq("*")) dependsOn (openmoleFile) settings (libraryDependencies += Libraries.equinoxOSGi) settings (thirdPartiesSettings: _*)
lazy val openmoleRandom = OsgiProject(thirdPartiesDir, "org.openmole.tool.random", imports = Seq("*")) settings (thirdPartiesSettings: _*) settings(libraryDependencies += Libraries.math, Libraries.addScalaLang(scalaVersionValue)) dependsOn (openmoleCache)
lazy val openmoleNetwork = OsgiProject(thirdPartiesDir, "org.openmole.tool.network", imports = Seq("*")) settings (thirdPartiesSettings: _*)
lazy val openmoleException = OsgiProject(thirdPartiesDir, "org.openmole.tool.exception", imports = Seq("*")) settings (thirdPartiesSettings: _*)
lazy val openmoleOutputRedirection = OsgiProject(thirdPartiesDir, "org.openmole.tool.outputredirection", imports = Seq("*")) settings (thirdPartiesSettings: _*)

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
  keyword,
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
  logconfig,
  outputManager,
  openmoleCompiler,
  project,
  openmoleBuildInfo,
  module,
  market,
  context,
  expansion,
  preference,
  db,
  threadProvider,
  services,
  location,
  code,
  networkService,
  timeService,
  csv,
  highlight,
  namespace,
  pluginRegistry)

lazy val keyword = OsgiProject(coreDir, "org.openmole.core.keyword", imports = Seq("*")) settings (coreSettings: _*) settings (
  defaultActivator,
  libraryDependencies ++= Libraries.monocle) dependsOn (pluginRegistry)

lazy val context = OsgiProject(coreDir, "org.openmole.core.context", imports = Seq("*")) settings(
  libraryDependencies ++= Seq(Libraries.cats, Libraries.sourceCode), defaultActivator
) dependsOn(tools, workspace, preference, pluginRegistry) settings (coreSettings: _*)

lazy val expansion = OsgiProject(coreDir, "org.openmole.core.expansion", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.cats)
  ) dependsOn(context, tools, openmoleRandom, openmoleFile, pluginManager, openmoleCompiler, code, exception) settings (coreSettings: _*)

lazy val workflow = OsgiProject(coreDir, "org.openmole.core.workflow", imports = Seq("*")) settings(
  libraryDependencies ++= Seq(Libraries.math, Libraries.cats, Libraries.equinoxOSGi, Libraries.shapeless),
  Libraries.addScalaLang(scalaVersionValue),
  defaultActivator
) dependsOn(
  event,
  exception,
  tools,
  workspace,
  pluginManager,
  serializer,
  outputManager,
  openmoleCompiler,
  context,
  preference,
  expansion,
  threadProvider,
  code,
  networkService,
  keyword,
  csv,
  pluginRegistry) settings (coreSettings: _*)

lazy val serializer = OsgiProject(coreDir, "org.openmole.core.serializer", global = true, imports = Seq("*")) settings(
  libraryDependencies += Libraries.xstream,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(workspace, pluginManager, fileService, tools, openmoleTar, openmoleCompiler) settings (coreSettings: _*)

lazy val communication = OsgiProject(coreDir, "org.openmole.core.communication", imports = Seq("*")) dependsOn(workflow, workspace) settings (coreSettings: _*)

lazy val openmoleDSL = OsgiProject(coreDir, "org.openmole.core.dsl", imports = Seq("*")) settings (
  libraryDependencies += Libraries.squants) dependsOn(workflow, logconfig, csv, pluginRegistry) settings (coreSettings: _*) settings (defaultActivator)

lazy val exception = OsgiProject(coreDir, "org.openmole.core.exception", imports = Seq("*")) settings (coreSettings: _*)

lazy val csv = OsgiProject(coreDir, "org.openmole.core.csv", imports = Seq("*")) dependsOn (context) settings (coreSettings: _*) settings (
  libraryDependencies += Libraries.opencsv)

lazy val tools = OsgiProject(coreDir, "org.openmole.core.tools", global = true, imports = Seq("*")) settings
  (libraryDependencies ++= Seq(Libraries.xstream, Libraries.exec, Libraries.math, Libraries.scalatest, Libraries.equinoxOSGi), Libraries.addScalaLang(scalaVersionValue)) dependsOn
  (exception, openmoleTar, openmoleFile, openmoleLock, openmoleThread, openmoleHash, openmoleLogger, openmoleStream, openmoleCollection, openmoleStatistics, openmoleTypes, openmoleCache, openmoleRandom, openmoleNetwork, openmoleException, openmoleOutputRedirection, openmoleLogger, openmoleTypes) settings (coreSettings: _*)

lazy val event = OsgiProject(coreDir, "org.openmole.core.event", imports = Seq("*")) dependsOn (tools) settings (coreSettings: _*)

lazy val code = OsgiProject(coreDir, "org.openmole.core.code", imports = Seq("*")) dependsOn(tools, workspace) settings (coreSettings: _*)

lazy val replication = OsgiProject(coreDir, "org.openmole.core.replication", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.slick, Libraries.xstream, Libraries.guava)) settings (coreSettings: _*) dependsOn(db, preference, workspace, openmoleCache)

lazy val db = OsgiProject(coreDir, "org.openmole.core.db", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.slick, Libraries.xstream, Libraries.h2, Libraries.scopt)) settings (coreSettings: _*) dependsOn(openmoleNetwork, exception, openmoleCrypto, openmoleFile, openmoleLogger)

lazy val preference = OsgiProject(coreDir, "org.openmole.core.preference", imports = Seq("*")) settings(
  libraryDependencies ++= Seq(Libraries.configuration, Libraries.squants), Libraries.addScalaLang(scalaVersionValue)) settings (coreSettings: _*) dependsOn(openmoleNetwork, openmoleCrypto, openmoleFile, openmoleThread, openmoleTypes, openmoleLock, exception, preferenceMacro)

lazy val preferenceMacro = OsgiProject(coreDir, "org.openmole.core.preferencemacro", imports = Seq("*")) settings(
  libraryDependencies ++= Seq(Libraries.configuration, Libraries.squants), Libraries.addScalaLang(scalaVersionValue)) settings (coreSettings: _*) dependsOn(openmoleNetwork, openmoleCrypto, openmoleFile, openmoleThread, openmoleTypes, openmoleLock, exception)

lazy val workspace = OsgiProject(coreDir, "org.openmole.core.workspace", imports = Seq("*")) dependsOn
  (exception, event, tools, openmoleCrypto) settings (coreSettings: _*)

lazy val authentication = OsgiProject(coreDir, "org.openmole.core.authentication", imports = Seq("*")) dependsOn(workspace, serializer) settings (coreSettings: _*)

lazy val services = OsgiProject(coreDir, "org.openmole.core.services", imports = Seq("*")) dependsOn(workspace, serializer, preference, fileService, networkService, threadProvider, replication, authentication, openmoleOutputRedirection, timeService) settings (coreSettings: _*)

lazy val location = OsgiProject(coreDir, "org.openmole.core.location", imports = Seq("*")) dependsOn (exception) settings (coreSettings: _*)

lazy val highlight = OsgiProject(coreDir, "org.openmole.core.highlight", imports = Seq("*")) dependsOn (exception) settings (coreSettings: _*)

lazy val namespace = OsgiProject(coreDir, "org.openmole.core.namespace", imports = Seq("*")) dependsOn (exception) settings (coreSettings: _*)

lazy val pluginManager = OsgiProject(
  coreDir,
  "org.openmole.core.pluginmanager",
  imports = Seq("*")
) settings (defaultActivator) dependsOn(exception, tools, location) settings (coreSettings: _*)

lazy val pluginRegistry = OsgiProject(coreDir, "org.openmole.core.pluginregistry", imports = Seq("*")) dependsOn(exception, highlight, namespace, preference) settings (coreSettings: _*)


lazy val fileService = OsgiProject(coreDir, "org.openmole.core.fileservice", imports = Seq("*")) dependsOn(tools, workspace, openmoleTar, preference, threadProvider, pluginRegistry) settings (coreSettings: _*) settings (defaultActivator) settings (libraryDependencies += Libraries.guava)

lazy val networkService = OsgiProject(coreDir, "org.openmole.core.networkservice", imports = Seq("*")) dependsOn(tools, workspace, preference, pluginRegistry) settings (coreSettings: _*) settings (defaultActivator)

lazy val timeService = OsgiProject(coreDir, "org.openmole.core.timeservice", imports = Seq("*"))  settings (coreSettings: _*)

lazy val threadProvider = OsgiProject(coreDir, "org.openmole.core.threadprovider", imports = Seq("*")) dependsOn(tools, preference, pluginRegistry) settings (coreSettings: _*) settings (defaultActivator)

lazy val module = OsgiProject(coreDir, "org.openmole.core.module", imports = Seq("*")) dependsOn(openmoleBuildInfo, expansion, openmoleHash, openmoleFile, pluginManager) settings (coreSettings: _*) settings(
  libraryDependencies ++= Libraries.gridscaleHTTP,
  libraryDependencies += Libraries.json4s,
  defaultActivator)

lazy val market = OsgiProject(coreDir, "org.openmole.core.market", imports = Seq("*")) enablePlugins (ScalaJSPlugin) dependsOn(openmoleBuildInfo, expansion, openmoleHash, openmoleFile, pluginManager) settings (coreSettings: _*) settings(
  libraryDependencies ++= Libraries.gridscaleHTTP,
  libraryDependencies += Libraries.json4s,
  defaultActivator)

lazy val logconfig = OsgiProject(
  coreDir,
  "org.openmole.core.logconfig",
  imports = Seq("*")
) settings(libraryDependencies ++= Seq(Libraries.log4j, Libraries.logback, Libraries.slf4j), defaultActivator) dependsOn (tools) settings (coreSettings: _*)

lazy val outputManager = OsgiProject(coreDir, "org.openmole.core.outputmanager", imports = Seq("*")) dependsOn (openmoleStream, openmoleTypes) settings (coreSettings: _*) settings (defaultActivator)

lazy val openmoleCompiler = OsgiProject(coreDir, "org.openmole.core.compiler", global = true, imports = Seq("*"), exports = Seq("org.openmole.core.compiler.*", "$line5.*")) dependsOn (pluginManager) settings(
  OsgiKeys.importPackage := Seq("*"),
  Libraries.addScalaLang(scalaVersionValue),
  libraryDependencies ++= Libraries.monocle,
  defaultActivator
) dependsOn(openmoleOSGi, workspace, fileService, openmoleTypes) settings (coreSettings: _*)

lazy val project = OsgiProject(coreDir, "org.openmole.core.project", imports = Seq("*")) dependsOn(namespace, openmoleCompiler, openmoleDSL, services) settings (OsgiKeys.importPackage := Seq("*")) settings (coreSettings: _*) settings (
  Libraries.addScalaLang(scalaVersionValue)
  )

lazy val openmoleBuildInfo = OsgiProject(coreDir, "org.openmole.core.buildinfo", imports = Seq("*")) enablePlugins (BuildInfoPlugin) settings(
  //sourceGenerators in Compile += buildInfo.taskValue,
  (Compile / sourceGenerators) := Seq(
    Def.taskDyn {
      val src = (Compile / sourceManaged).value
      val buildInfoDirectory = src / "sbt-buildinfo"
      if (buildInfoDirectory.exists && !buildInfoDirectory.list().isEmpty) Def.task {
        buildInfoDirectory.listFiles.toSeq
      }
      else (Compile / buildInfo)
    }.taskValue
  ),
  buildInfoKeys :=
    Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      },
      BuildInfoKey.action("splash") {
        import sys.process._
        import _root_.scala.util.Try

        val banner = s"OpenMOLE ${version.value.takeWhile(_ != '.')}"
        Try { s"figlet $banner" !! }.toOption.getOrElse(banner)
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
    allEnvironment ++
    allDomain ++
    allTools

def allTools = Seq(netLogoAPI, netLogo5API, netLogo6API, pattern, json, methodData)

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

lazy val pattern = OsgiProject(pluginDir, "org.openmole.plugin.tool.pattern", imports = Seq("*")) dependsOn(exception, openmoleDSL) settings (toolsSettings: _*) settings (defaultActivator)


lazy val json = OsgiProject(pluginDir, "org.openmole.plugin.tool.json", imports = Seq("*")) dependsOn(exception, openmoleDSL) settings (toolsSettings: _*) settings(
  libraryDependencies += Libraries.json4s,
  libraryDependencies += Libraries.shapeless)


lazy val methodData = OsgiProject(pluginDir, "org.openmole.plugin.tool.methoddata", imports = Seq("*")) settings (toolsSettings: _*) settings (
  OsgiKeys.bundleActivator := None,
  libraryDependencies += Libraries.circe
) enablePlugins(ScalaJSPlugin) dependsOn(openmoleDSL)

/* Domain */

def allDomain = Seq(collectionDomain, distributionDomain, fileDomain, modifierDomain, rangeDomain, boundsDomain)

lazy val collectionDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.collection", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val distributionDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.distribution", imports = Seq("*")) dependsOn (openmoleDSL) settings
  (libraryDependencies ++= Seq(Libraries.math)) settings (pluginSettings: _*)

lazy val fileDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.file", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val modifierDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.modifier", imports = Seq("*")) dependsOn (openmoleDSL, rangeDomain % "test", fileDomain % "test") settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings: _*)

lazy val rangeDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.range", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val boundsDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.bounds", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)


/* Environment */

def allEnvironment = Seq(batch, gridscale, ssh, egi, pbs, oar, sge, condor, slurm, dispatch)

lazy val batch = OsgiProject(pluginDir, "org.openmole.plugin.environment.batch", imports = Seq("*")) dependsOn(
  workflow, workspace, tools, event, replication, exception,
  serializer, fileService, pluginManager, openmoleTar, communication, authentication, location, services,
  openmoleByteCode
) settings (
  libraryDependencies ++= Seq(
    Libraries.gridscale,
    Libraries.h2,
    Libraries.guava,
    Libraries.jasypt,
    Libraries.slick
  )
  ) settings (pluginSettings: _*)


//lazy val cluster = OsgiProject(pluginDir, "org.openmole.plugin.environment.cluster", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings (pluginSettings: _*)

lazy val oar = OsgiProject(pluginDir, "org.openmole.plugin.environment.oar", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleOAR) settings (pluginSettings: _*)


lazy val egi = OsgiProject(pluginDir, "org.openmole.plugin.environment.egi") dependsOn(openmoleDSL, batch, workspace, fileService, gridscale) settings(
  libraryDependencies ++= Libraries.gridscaleEGI, Libraries.addScalaLang(scalaVersionValue)) settings (pluginSettings: _*)

lazy val gridscale = OsgiProject(pluginDir, "org.openmole.plugin.environment.gridscale", imports = Seq("*")) settings (
  libraryDependencies += Libraries.gridscaleLocal) dependsOn(openmoleDSL, tools, batch, exception) settings (pluginSettings: _*)

lazy val pbs = OsgiProject(pluginDir, "org.openmole.plugin.environment.pbs", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscalePBS) settings (pluginSettings: _*)

lazy val sge = OsgiProject(pluginDir, "org.openmole.plugin.environment.sge", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleSGE) settings (pluginSettings: _*)

lazy val condor = OsgiProject(pluginDir, "org.openmole.plugin.environment.condor", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleCondor) settings (pluginSettings: _*)

lazy val slurm = OsgiProject(pluginDir, "org.openmole.plugin.environment.slurm", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings
  (libraryDependencies += Libraries.gridscaleSLURM) settings (pluginSettings: _*)

lazy val ssh = OsgiProject(pluginDir, "org.openmole.plugin.environment.ssh", imports = Seq("*")) dependsOn(openmoleDSL, event, batch, gridscale) settings
  (libraryDependencies ++= Libraries.gridscaleSSH) settings (pluginSettings: _*)

lazy val dispatch = OsgiProject(pluginDir, "org.openmole.plugin.environment.dispatch", imports = Seq("*")) dependsOn(openmoleDSL, event, batch, gridscale) settings (pluginSettings: _*)

/* Hook */

def allHook = Seq(displayHook, fileHook, modifierHook, jsonHook, omrHook)

lazy val displayHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.display", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val fileHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.file", imports = Seq("*")) dependsOn(openmoleDSL, replication % "test") settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings: _*)

lazy val modifierHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.modifier", imports = Seq("*")) dependsOn (openmoleDSL) settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings: _*)

lazy val jsonHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.json", imports = Seq("*")) dependsOn(openmoleDSL, json, replication % "test") settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings: _*)

lazy val omrHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.omr", imports = Seq("*")) dependsOn(openmoleDSL, jsonHook, openmoleBuildInfo, replication % "test") settings(
  libraryDependencies += Libraries.scalatest, libraryDependencies += Libraries.circe) settings (pluginSettings: _*)


/* Method */

def allMethod = Seq(evolution, directSampling, sensitivity, abc)

lazy val evolution = OsgiProject(pluginDir, "org.openmole.plugin.method.evolution", imports = Seq("*")) dependsOn(
  openmoleDSL, toolsTask, pattern, evolutionData, collectionDomain % "test", boundsDomain % "test"
) settings(
  libraryDependencies += Libraries.mgo,
  libraryDependencies += Libraries.shapeless,
  libraryDependencies += Libraries.circe) settings (pluginSettings: _*)

lazy val evolutionData = OsgiProject(pluginDir, "org.openmole.plugin.method.evolution.data", imports = Seq("*")) settings (pluginSettings: _*) settings (
  OsgiKeys.bundleActivator := None,
  libraryDependencies += Libraries.circe
) enablePlugins(ScalaJSPlugin) dependsOn(omrHook, methodData)


lazy val abc = OsgiProject(pluginDir, "org.openmole.plugin.method.abc", imports = Seq("*")) dependsOn(openmoleDSL, toolsTask, pattern, boundsDomain % "test") settings(
  libraryDependencies += Libraries.mgo, libraryDependencies += Libraries.shapeless) settings (pluginSettings: _*)

lazy val directSampling = OsgiProject(pluginDir, "org.openmole.plugin.method.directsampling", imports = Seq("*")) dependsOn(openmoleDSL, distributionDomain, pattern, modifierDomain, fileHook, combineSampling, omrHook, methodData) settings (pluginSettings: _*)

lazy val sensitivity = OsgiProject(pluginDir, "org.openmole.plugin.method.sensitivity", imports = Seq("*")) dependsOn(exception, workflow, workspace, openmoleDSL, lhsSampling, quasirandomSampling, directSampling, collectionDomain % "test", boundsDomain % "test") settings (pluginSettings: _*)


/* Sampling */

def allSampling = Seq(combineSampling, csvSampling, oneFactorSampling, lhsSampling, quasirandomSampling)

lazy val combineSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.combine", imports = Seq("*")) dependsOn(exception, modifierDomain, collectionDomain, workflow) settings (pluginSettings: _*)

lazy val csvSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.csv", imports = Seq("*")) dependsOn(exception, workflow) settings (
  libraryDependencies += Libraries.scalatest
  ) settings (pluginSettings: _*)

lazy val oneFactorSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.onefactor", imports = Seq("*")) dependsOn(exception, workflow, openmoleDSL, combineSampling, collectionDomain) settings (pluginSettings: _*)

lazy val lhsSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.lhs", imports = Seq("*")) dependsOn(exception, workflow, workspace, openmoleDSL) settings (pluginSettings: _*)

lazy val quasirandomSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.quasirandom", imports = Seq("*")) dependsOn(exception, workflow, workspace, openmoleDSL) settings (
  libraryDependencies += Libraries.math
  ) settings (pluginSettings: _*)


/* Source */

def allSource = Seq(fileSource)

lazy val fileSource = OsgiProject(pluginDir, "org.openmole.plugin.source.file", imports = Seq("*")) dependsOn(openmoleDSL, serializer, exception) settings (pluginSettings: _*)


/* Task */

def allTask = Seq(toolsTask, external, netLogo, netLogo5, netLogo6, jvm, scala, template, systemexec, container, r, scilab, python, gama, cormas, spatial, timing)

lazy val toolsTask = OsgiProject(pluginDir, "org.openmole.plugin.task.tools", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val external = OsgiProject(pluginDir, "org.openmole.plugin.task.external", imports = Seq("*")) dependsOn(openmoleDSL, workspace) settings (pluginSettings: _*)

lazy val netLogo = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo", imports = Seq("*")) dependsOn(openmoleDSL, external, netLogoAPI) settings (pluginSettings: _*)

lazy val netLogo5 = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo5") dependsOn(netLogo, openmoleDSL, external, netLogo5API) settings (pluginSettings: _*)

lazy val netLogo6 = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo6") dependsOn(netLogo, openmoleDSL, external, netLogo6API) settings (pluginSettings: _*)

lazy val jvm = OsgiProject(pluginDir, "org.openmole.plugin.task.jvm", imports = Seq("*")) dependsOn(openmoleDSL, external, workspace) settings (pluginSettings: _*)

lazy val scala = OsgiProject(pluginDir, "org.openmole.plugin.task.scala", imports = Seq("*")) dependsOn(openmoleDSL, jvm, openmoleCompiler) settings (pluginSettings: _*)

lazy val template = OsgiProject(pluginDir, "org.openmole.plugin.task.template", imports = Seq("*")) dependsOn(openmoleDSL, replication % "test") settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings: _*)

lazy val systemexec = OsgiProject(pluginDir, "org.openmole.plugin.task.systemexec", imports = Seq("*")) dependsOn(openmoleDSL, external, workspace) settings (
  libraryDependencies += Libraries.exec) settings (pluginSettings: _*)

lazy val container = OsgiProject(pluginDir, "org.openmole.plugin.task.container", imports = Seq("*")) dependsOn(openmoleFile, pluginManager, external, expansion, exception) settings (pluginSettings: _*) settings (
  libraryDependencies += Libraries.container)

lazy val r = OsgiProject(pluginDir, "org.openmole.plugin.task.r", imports = Seq("*")) dependsOn(container, json) settings (pluginSettings: _*)

lazy val scilab = OsgiProject(pluginDir, "org.openmole.plugin.task.scilab", imports = Seq("*")) dependsOn (container) settings (pluginSettings: _*)

lazy val python = OsgiProject(pluginDir, "org.openmole.plugin.task.python", imports = Seq("*")) dependsOn(container, json) settings (pluginSettings: _*)

lazy val gama = OsgiProject(pluginDir, "org.openmole.plugin.task.gama", imports = Seq("*")) dependsOn (container) settings (pluginSettings: _*)

lazy val cormas = OsgiProject(pluginDir, "org.openmole.plugin.task.cormas", imports = Seq("*")) dependsOn (container, json) settings (pluginSettings: _*) settings(
  libraryDependencies += Libraries.json4s)

lazy val timing = OsgiProject(pluginDir, "org.openmole.plugin.task.timing", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings: _*)

lazy val spatial = OsgiProject(pluginDir, "org.openmole.plugin.task.spatial", imports = Seq("*")) dependsOn(openmoleDSL) settings(
  libraryDependencies += Libraries.math,
  libraryDependencies += Libraries.spatialsampling
) settings (pluginSettings: _*)

/* ---------------- REST ------------------- */


def restDir = file("rest")

lazy val message = OsgiProject(restDir, "org.openmole.rest.message") settings (defaultSettings: _*)

lazy val server = OsgiProject(
  restDir,
  "org.openmole.rest.server",
  imports = Seq("org.h2", "!com.sun.*", "*"),
  dynamicImports = Seq("org.eclipse.jetty.*")
) dependsOn(workflow, openmoleTar, openmoleCollection, project, message, openmoleCrypto, services, module) settings(
  libraryDependencies ++= Seq(Libraries.bouncyCastle, Libraries.logback, Libraries.scalatra, Libraries.arm, Libraries.codec, Libraries.json4s), Libraries.addScalaLang(scalaVersionValue)) settings (defaultSettings: _*)


/* -------------------- GUI --------------------- */


def guiDir = file("gui")

def guiExt = guiDir / "ext"
def guiExtTarget = guiExt / "target"


/* -------------------- Ext ----------------------*/

lazy val dataGUI = OsgiProject(guiExt, "org.openmole.gui.ext.data") enablePlugins (ScalaJSPlugin) settings(
  Libraries.scalaTagsJS,
  Libraries.scalajsDomJS,
  libraryDependencies ++= Libraries.monocle
) settings (defaultSettings: _*)

lazy val extServer = OsgiProject(guiExt, "org.openmole.gui.ext.server") dependsOn(dataGUI, workspace, module, services) settings(
  libraryDependencies += Libraries.autowire,
  libraryDependencies += Libraries.boopickle,
  libraryDependencies += Libraries.equinoxOSGi
) settings (defaultSettings: _*)

lazy val extClient = OsgiProject(guiExt, "org.openmole.gui.ext.client") enablePlugins (ScalaJSPlugin) dependsOn(dataGUI, sharedGUI) settings(
  Libraries.boopickleJS,
  Libraries.autowireJS,
  Libraries.rxJS,
  Libraries.scalajsDomJS,
  Libraries.scaladgetTools,
  Libraries.bootstrapnative,
  Libraries.scalaTagsJS
) settings (defaultSettings: _*)

lazy val sharedGUI = OsgiProject(guiExt, "org.openmole.gui.ext.api") dependsOn(dataGUI, market) settings (defaultSettings: _*)

lazy val jsCompile = OsgiProject(guiServerDir, "org.openmole.gui.server.jscompile", imports = Seq("*")) dependsOn(pluginManager, fileService, workspace, dataGUI) settings (defaultSettings: _*) settings(
  libraryDependencies += "org.scala-js" %%% "scalajs-library" % scalajsVersion % "provided" intransitive(),
  libraryDependencies += "org.scala-lang.modules" %%% "scala-collection-compat" % "2.1.4" % "provided" intransitive(),

  libraryDependencies += Libraries.scalajsLogging,
  libraryDependencies += Libraries.scalajsLinker,

  (Compile / resourceDirectories) += (crossTarget.value / "resources"),
  (OsgiKeys.embeddedJars) := {
    val scalaLib =
      (Compile / Keys.externalDependencyClasspath).value.filter {
        d => d.data.getName startsWith "scalajs-library"
      }.head

    val dest = crossTarget.value / "resources/scalajs-library.jar"
    dest.getParentFile.mkdirs()
    sbt.IO.copyFile(scalaLib.data, dest)
    Seq()
  })

def guiClientDir = guiDir / "client"
lazy val clientGUI = OsgiProject(guiClientDir, "org.openmole.gui.client.core") enablePlugins (ExecNpmPlugin) dependsOn
  (sharedGUI, clientToolGUI, market, dataGUI, extClient) settings(
  libraryDependencies += Libraries.async,
  Compile / npmDeps += Dep("ace-builds/src-min", "1.4.3", List("mode-scala.js", "theme-github.js", "ext-language_tools.js"), true),
  Compile / npmDeps += Dep("sortablejs", "1.10.2", List("Sortable.min.js"))
) settings (defaultSettings: _*)


lazy val clientToolGUI = OsgiProject(guiClientDir, "org.openmole.gui.client.tool", privatePackages = Seq("autowire.*", "boopickle.*", "sourcecode.*", "rx.*", "org.scalajs.dom.*", "scalatags.*", "scaladget.*", "net.scalapro.sortable.*", "org.openmole.plotlyjs.*", "org.querki.jsext.*", "scala.collection.compat.*")) enablePlugins (ScalaJSPlugin) dependsOn (workspace) settings(
  Libraries.autowireJS,
  Libraries.boopickleJS,
  Libraries.scalajsDomJS,
  Libraries.scalaTagsJS,
  Libraries.ace,
  Libraries.bootstrapnative,
  Libraries.scaladgetTools,
  Libraries.rxJS,
  Libraries.sortable,
  Libraries.plotlyJS,
  Libraries.scalaCompatJS) dependsOn (extClient) settings (defaultSettings: _*)


/* -------------------------- Server ----------------------- */

def guiServerDir = guiDir / "server"

lazy val serverGUI = OsgiProject(guiServerDir, "org.openmole.gui.server.core", dynamicImports = Seq("org.eclipse.jetty.*")) settings
  (libraryDependencies ++= Seq(Libraries.arm, Libraries.autowire, Libraries.boopickle, Libraries.circe, Libraries.scalaTags, Libraries.logback, Libraries.scalatra, Libraries.clapper, Libraries.opencsv)) dependsOn(
  sharedGUI,
  dataGUI,
  workflow,
  openmoleBuildInfo,
  openmoleFile,
  openmoleTar,
  openmoleHash,
  openmoleCollection,
  project,
  openmoleDSL,
  batch,
  omrHook,
  openmoleStream,
  txtmark,
  openmoleCrypto,
  module,
  market,
  extServer,
  jsCompile,
  services,
  location
) settings (defaultSettings: _*)

/* -------------------- GUI Plugin ----------------------- */

def guiPluginSettings = defaultSettings ++ Seq(defaultActivator)
def guiStrictImports = Seq("!org.scalajs.*", "!rx.*", "!scala.scalajs.*", "!scaladget.*", "!org.openmole.plotlyjs.*", "!org.querki.*", "*")

def guiPluginDir = guiDir / "plugins"

lazy val guiEnvironmentEGIPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.egi") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  Libraries.bootstrapnative
) dependsOn(extServer, extClient, dataGUI, workspace, egi) enablePlugins (ScalaJSPlugin)

lazy val guiEnvironmentSSHKeyPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.sshkey") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  Libraries.bootstrapnative
) dependsOn(extServer, extClient, dataGUI, workspace, ssh) enablePlugins (ScalaJSPlugin)

lazy val guiEnvironmentSSHLoginPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.sshlogin") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  Libraries.bootstrapnative
) dependsOn(extServer, extClient, dataGUI, workspace, ssh) enablePlugins (ScalaJSPlugin)

lazy val netlogoWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.netlogo") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(extServer, extClient, extServer, workspace) enablePlugins (ScalaJSPlugin)

lazy val nativeWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.native") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  libraryDependencies += Libraries.arm
) dependsOn(extServer, extClient, extServer, workspace) enablePlugins (ScalaJSPlugin)

lazy val rWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.r") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi,
) dependsOn(extServer, extClient, extServer, workspace) enablePlugins (ScalaJSPlugin)

lazy val jarWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.jar") settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi,
) dependsOn(extServer, extClient, extServer, workspace) enablePlugins (ScalaJSPlugin)

lazy val evolutionAnalysisPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.analysis.evolution", imports = guiStrictImports) settings(
  guiPluginSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  Libraries.plotlyJS
) dependsOn(extServer, extClient, extServer, workspace, evolution) enablePlugins (ScalaJSPlugin)

def guiPlugins = Seq(
  guiEnvironmentSSHLoginPlugin,
  guiEnvironmentSSHKeyPlugin,
  guiEnvironmentEGIPlugin,
  netlogoWizardPlugin,
  nativeWizardPlugin,
  rWizardPlugin,
  jarWizardPlugin,
  evolutionAnalysisPlugin) //, guiEnvironmentDesktopGridPlugin)

/* -------------------- Bin ------------------------- */

def binDir = file("bin")


def bundleFilter(m: ModuleID, artifact: Artifact) = {
  //def excludedLibraryDependencies = Set("slick", "squants", "shapeless", "sourcecode", "eddsa", "sshj")

  def exclude = false
/*    (m.organization != "org.openmole.library" && excludedLibraryDependencies.exists(m.name.contains)) ||
      (m.name contains "scala-xml") ||
      (m.name contains "protobuf") ||
      (m.name contains "jts-core") || (m.name contains "si-quantity") || (m.name contains "systems-common-java8") || (m.name contains "uom-lib-common") || (m.name contains "unit-api") || (m.name contains "uom-se") // geotools bundled dependancies */

  def includeOtherBundles = Set[String]("scala-parser-combinators")

  def include = 
    (artifact.`type` == "bundle" && m.name != "osgi" && m.organization == "org.openmole.library") ||
      m.organization == "org.bouncycastle" ||
      (m.name == "httpclient-osgi") || 
      (m.name == "httpcore-osgi") ||
      (m.organization == "org.osgi" && m.name != "osgi") ||
      includeOtherBundles.exists(m.name.contains)

  include && !exclude
}

def noDependencyFilter(m: ModuleID, artifact: Artifact) = false

def rename(m: ModuleID): String = {
  val versionPattern = "([0-9]*)\\.([0-9]*).*".r

  val revision =
    m.revision match {
      case versionPattern(major, minor) => s"$major.$minor"
      case s => s
    }

  s"${m.organization.replaceAllLiterally(".", "-")}-${m.name}_${m.revision}.jar"
}


import Assembly._

lazy val openmoleUI = OsgiProject(binDir, "org.openmole.ui", singleton = true, imports = Seq("*")) settings (
  organization := "org.openmole.ui"
  ) dependsOn(
  openmoleCompiler,
  workspace,
  replication,
  exception,
  tools,
  event,
  pluginManager,
  workflow,
  serverGUI,
  clientGUI,
  logconfig,
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
    scala
  )

def openmoleNakedDependencies = allCore ++ Seq(openmoleUI) ++ minimumPlugins

def openmoleDependencies = openmoleNakedDependencies ++ corePlugins ++ guiPlugins

def requieredRuntimeLibraries = Seq(Libraries.osgiCompendium, Libraries.logging)

lazy val openmoleNaked =
  Project("openmole-naked", binDir / "openmole-naked") settings (assemblySettings: _*) enablePlugins (ExecNpmPlugin) settings(
    setExecutable ++= Seq("openmole", "openmole.bat"),
    Compile / Osgi.bundleDependencies := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject, includeRoot = false))).value,
    resourcesAssemble += (Compile / resourceDirectory).value -> assemblyPath.value,
    resourcesAssemble += ((serverGUI / Compile / resourceDirectory).value / "webapp") → (assemblyPath.value / "webapp"),
    resourcesAssemble += (clientGUI / Compile / dependencyFile).value -> (assemblyPath.value / "webapp/js/deps.js"),
    resourcesAssemble += (clientGUI / Compile / cssFile).value -> (assemblyPath.value / "webapp/css/"),
    resourcesAssemble += {
      val tarFile = (openmoleRuntime / tar).value
      tarFile -> (assemblyPath.value / "runtime" / tarFile.getName)
    },
    resourcesAssemble += (launcher / assemble).value -> (assemblyPath.value / "launcher"),
    resourcesAssemble ++= (Compile / Osgi.bundleDependencies).value.map(b ⇒ b → (assemblyPath.value / "plugins" / b.getName)),
    libraryDependencies ++= requieredRuntimeLibraries,
    dependencyFilter := bundleFilter,
    dependencyName := rename,
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    tarName := "openmole-naked.tar.gz",
    tarInnerFolder := "openmole",
    cleanFiles ++= (launcher / cleanFiles).value,
    cleanFiles ++= (openmoleRuntime / cleanFiles).value) dependsOn (toDependencies(openmoleNakedDependencies): _*) settings (defaultSettings: _*)

lazy val openmole =
  Project("openmole", binDir / "openmole") enablePlugins (TarPlugin) settings (assemblySettings: _*) settings (defaultSettings: _*) settings(
    setExecutable ++= Seq("openmole", "openmole.bat"),
    Compile / Osgi.bundleDependencies := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject, includeRoot = false))).value,
    tarName := "openmole.tar.gz",
    tarInnerFolder := "openmole",
    dependencyFilter := bundleFilter,
    dependencyName := rename,
    resourcesAssemble += (openmoleNaked / assemble).value -> assemblyPath.value,
    resourcesAssemble ++= (Compile / Osgi.bundleDependencies).value.map(b ⇒ b → (assemblyPath.value / "plugins" / b.getName)),
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    cleanFiles ++= (openmoleNaked / cleanFiles).value) dependsOn (toDependencies(openmoleDependencies): _*)

lazy val openmoleRuntime =
  OsgiProject(binDir, "org.openmole.runtime", singleton = true, imports = Seq("*")) enablePlugins (TarPlugin) settings (assemblySettings: _*) dependsOn(workflow, communication, serializer, logconfig, event, exception) settings(
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    resourcesAssemble += (Compile / resourceDirectory).value -> assemblyPath.value,
    resourcesAssemble += (launcher / assemble).value -> (assemblyPath.value / "launcher"),
    resourcesAssemble ++= (Compile / Osgi.bundleDependencies).value.map(b ⇒ b → (assemblyPath.value / "plugins" / b.getName)),
    setExecutable ++= Seq("run.sh"),
    tarName := "runtime.tar.gz",
    libraryDependencies ++= requieredRuntimeLibraries,
    libraryDependencies += Libraries.scopt,
    dependencyFilter := bundleFilter,
    dependencyName := rename
  ) dependsOn (toDependencies(allCore): _*) settings (defaultSettings: _*)



lazy val api = Project("api", binDir / "target" / "api") settings (defaultSettings: _*) enablePlugins (ScalaUnidocPlugin) settings (
  //compile := sbt.inc.Analysis.Empty,
  ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(openmoleDependencies.map(p ⇒ p: ProjectReference): _*)
  // -- inProjects(Libraries.projects.map(p ⇒ p: ProjectReference) ++ ThirdParties.projects.map(p ⇒ p: ProjectReference)*/
  //  Tar.name := "openmole-api.tar.gz",
  //  Tar.folder := (UnidocKeys.unidoc in Compile).map(_.head).value
  )


lazy val site = crossProject(JSPlatform, JVMPlatform).in(binDir / "org.openmole.site") settings (defaultSettings: _*) jvmSettings (scalatex.SbtPlugin.projectSettings) jvmSettings(
  libraryDependencies += Libraries.scalatexSite,
  libraryDependencies += Libraries.json4s,
  libraryDependencies += Libraries.spray,
  libraryDependencies += Libraries.txtmark,
  libraryDependencies += Libraries.scalaTags,
  libraryDependencies += Libraries.scalajHttp,
) jsSettings(
  Libraries.rxJS,
  Libraries.bootstrapnative,
  Libraries.lunr,
  Libraries.scaladgetTools,
  Libraries.scalajsDomJS,
  Libraries.highlightJS
)

lazy val siteJS = site.js enablePlugins (ExecNpmPlugin) settings (test := {}, scalacOptions := Seq())
lazy val siteJVM = site.jvm dependsOn(tools, project, serializer, openmoleBuildInfo, marketIndex) settings (
  libraryDependencies += Libraries.sourceCode)

lazy val cloneMarket = taskKey[Unit]("cloning market place")
lazy val defineMarketBranch = taskKey[Option[String]]("define market place branch")

lazy val marketIndex = Project("marketindex", binDir / "org.openmole.marketindex") settings (defaultSettings: _*) settings(
  libraryDependencies += Libraries.json4s,
  defineMarketBranch := {
    val OMversion = version.value
    val v = OMversion.split('.').headOption.map(v => s"$v-dev")
    assert(v.isDefined)
    v
  },
  cloneMarket := {
    val runner = git.runner.value
    val dir = baseDirectory.value / "target/openmole-market"
    val marketBranch = defineMarketBranch.value
    runner.updated("https://gitlab.openmole.org/openmole/market.git", marketBranch, dir, ConsoleLogger())
  }
) dependsOn(openmoleBuildInfo, openmoleFile, openmoleTar, market)

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
    val defaultDest = (siteJVM / target).value / "site"
    val (siteTarget, args) = parse("--target", defaultDest, parsed)

    (siteJVM / Compile / run).toTask(" " + args.mkString(" ")).map(_ => siteTarget)
  }.evaluated

  def copySiteResources(siteBuildJS: File, dependencyFile: File, resourceDirectory: File, siteTarget: File, cssFile: File) = {
    IO.copyFile(siteBuildJS, siteTarget / "js/sitejs.js")
    IO.copyFile(dependencyFile, siteTarget / "js/deps.js")
    IO.copyDirectory(resourceDirectory / "js", siteTarget / "js")
    IO.copyDirectory(resourceDirectory / "css", siteTarget / "css")
    IO.copyDirectory(cssFile, siteTarget / "css")
    IO.copyDirectory(resourceDirectory / "fonts", siteTarget / "fonts")
    IO.copyDirectory(resourceDirectory / "img", siteTarget / "img")
    IO.copyDirectory(resourceDirectory / "bibtex", siteTarget / "bibtex")
    IO.copyDirectory(resourceDirectory / "script", siteTarget / "script")
    IO.copyDirectory(resourceDirectory / "paper", siteTarget / "paper")
  }

  copySiteResources((siteJS / Compile / fullOptJS).value.data,
    (siteJS / Compile / dependencyFile).value,
    (siteJVM / Compile / resourceDirectory).value,
    siteTarget,
    (siteJS / Compile / cssFile).value)

  siteTarget
}

lazy val buildOpenMOLE = inputKey[File]("buildOpenMOLE")
buildOpenMOLE := {
  (openmole / assemble).value
}

def siteTests = Def.taskDyn {
  val testTarget = (siteJVM / target).value / "tests"
  IO.delete(testTarget)
  (siteJVM / Compile / run).toTask(" --test --target " + testTarget).map(_ => testTarget)
}

lazy val tests = Project("tests", binDir / "tests") settings (defaultSettings: _*) settings (assemblySettings: _*) settings(
  resourcesAssemble += (siteTests.value -> (assemblyPath.value / "tests")),
  dependencyFilter := noDependencyFilter
)

lazy val testSiteClean = inputKey[Unit]("testSiteClean")
testSiteClean := {
  (tests / clean).value
}

lazy val testSite = inputKey[Unit]("testSite")
testSite := {
  import _root_.scala.sys.process._

  val ret =
    Process(
      Seq(
        ((openmole / assemble).value / "openmole").getAbsolutePath,
        "--test-compile",
        ((tests / assemble).value / "tests").getAbsolutePath)
    ) !

  if (ret != 0) sys.error("Some tests have failed")
  else sLog.value.info("All tests successful")
}


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
    resourcesAssemble ++= (Compile / Osgi.bundleDependencies).value.map(b ⇒ b → (assemblyPath.value / "plugins" / b.getName)),
    resourcesAssemble += ((Compile / resourceDirectory).value / "modules") -> (assemblyPath.value / "modules"),
    resourcesAssemble += (launcher / assemble).value -> (assemblyPath.value / "launcher"),
    libraryDependencies ++= requieredRuntimeLibraries,
    dependencyFilter := bundleFilter,
    dependencyName := rename) dependsOn (toDependencies(openmoleNakedDependencies): _*) dependsOn (toDependencies(openmoleDependencies): _*)


lazy val launcher = OsgiProject(binDir, "org.openmole.launcher", imports = Seq("*"), settings = assemblySettings) settings(
  autoScalaLibrary := false,
  libraryDependencies += Libraries.equinoxOSGi,
  resourcesAssemble += {
    val bundle = (OsgiKeys.bundle).value
    bundle -> (assemblyPath.value / bundle.getName)
  }
) settings (defaultSettings: _*)


lazy val consoleBin = OsgiProject(binDir, "org.openmole.console", imports = Seq("*")) settings (
  libraryDependencies += Libraries.boopickle
  ) dependsOn(
  workflow,
  openmoleCompiler,
  project,
  openmoleDSL,
  openmoleBuildInfo,
  module
) settings (defaultSettings: _*)

val generateDocker = taskKey[Unit]("Prepare the docker build")

lazy val dockerBin = Project("docker", binDir / "docker") enablePlugins (sbtdocker.DockerPlugin) settings(
  docker / imageNames := Seq(
    ImageName("openmole/openmole:latest"),

    ImageName(
      namespace = Some("openmole"),
      repository = "openmole",
      tag = Some(version.value)
    )
  ),
  docker / dockerfile := new Dockerfile {
    from("debian:testing")
    maintainer("Romain Reuillon <romain.reuillon@iscpif.fr>, Jonathan Passerat-Palmbach <j.passerat-palmbach@imperial.ac.uk>")
    copy((openmole / assemble).value, s"/openmole")
    runRaw(
      """cp /etc/apt/sources.list /etc/apt/sources.list.d/unsable.list && sed -i "s/testing/unstable/g" /etc/apt/sources.list.d/unsable.list && \
       apt-get update && \
       apt-get install --no-install-recommends -y ca-certificates default-jre-headless ca-certificates-java bash tar gzip sudo locales singularity-container && \
       apt-get clean autoclean && apt-get autoremove --yes && rm -rf /var/lib/{apt,dpkg,cache,log}/ /var/lib/apt/lists/* && \
       mkdir -p /lib/modules""")
    runRaw(
      """sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
        |dpkg-reconfigure --frontend=noninteractive locales && \
        |update-locale LANG=en_US.UTF-8""".stripMargin
    )
    env("LC_ALL", "en_US.UTF-8")
    env("LANG", "en_US.UTF-8")
    env("LANGUAGE", "en_US.UTF-8")
    runRaw(
      """groupadd -r openmole && \
         useradd -r -g openmole openmole --home-dir /var/openmole/ --create-home && \
         chown openmole:openmole -R /var/openmole""")
    runRaw(
      """chmod +x /openmole/openmole && \
        |ln -s /openmole/openmole /usr/bin/openmole""".stripMargin)
    runRaw(
      """echo '#!/bin/bash' > /usr/bin/openmole-docker && \
        |echo 'export HOME=/var/openmole && mkdir -p $HOME && chown openmole:openmole $HOME && sudo -u openmole openmole --http --mem 2G --port 8080 --remote $@' >>/usr/bin/openmole-docker && \
        |chmod +x-w /usr/bin/openmole-docker""".stripMargin)
    volume("/var/openmole")
    expose(8443)
    cmdShell("openmole-docker")
  },
  generateDocker := {
    val dockerDir = target.value / "docker"
    val dockerFile = (docker / dockerfile).value.asInstanceOf[Dockerfile]
    val stagedDockerfile =  sbtdocker.staging.DefaultDockerfileProcessor(dockerFile, dockerDir)
    IO.write(dockerDir / "Dockerfile",stagedDockerfile.instructionsString)
    stagedDockerfile.stageFiles.foreach {
      case (source, destination) => source.stage(destination)
    }
  }
)






