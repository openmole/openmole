import org.openmole.buildsystem._
import OMKeys._
import sbt.{addSbtPlugin, io, _}
import Keys.{libraryDependencies, _}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

import _root_.openmole.common._

organization := "org.openmole"
name := "openmole-root"

//import com.typesafe.sbt.SbtScalariform.ScalariformKeys
//import scalariform.formatter.preferences._

/*def formatSettings =
  Seq(
    ScalariformKeys.preferences :=
      ScalariformKeys.preferences(p =>
        p.setPreference(AlignParameters, true)
          .setPreference(AlignSingleLineCaseStatements, true)
          .setPreference(DanglingCloseParenthesis, Preserve)
          .setPreference(CompactControlReadability, true)
      ).value,
    scalariformAutoformat := true
  )*/

//def scala2(scalaVersion: String): Boolean =
//  CrossVersion.partialVersion(scalaVersion) match {
//    case Some((2, _))  => true
//    case _             => false
//  }

Global / concurrentRestrictions := Seq(
  Tags.limitAll(6)
)

def commonSettings =
  Seq(
    organization := "org.openmole",
    updateOptions := updateOptions.value.withCachedResolution(true),
    resolvers += DefaultMavenRepository,
    resolvers ++= Resolver.sonatypeOssRepos("releases"),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    resolvers ++= Resolver.sonatypeOssRepos("staging"),
    javacOptions ++= Seq("-source", "21", "-target", "21"), //, "-J-Djdk.util.zip.disableZip64ExtraFieldValidation=true"),
    install / packageDoc / publishArtifact := false,
    install / packageSrc / publishArtifact := false,
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " },
    libraryDependencies += Libraries.scalatest,
    Test / fork := true,
    Test / javaOptions ++= Seq("-Xss2M", "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED") //, "-Djdk.util.zip.disableZip64ExtraFieldValidation=true")
  )

def scala3Settings =
  commonSettings ++
    Seq(
      Global / scalaVersion := scala3VersionValue, // + "-bin-typelevel-4",
      scalacOptions ++= Seq("-java-output-version:21", "-language:higherKinds", "-language:postfixOps", "-language:implicitConversions", "-Xmax-inlines:100"), // "-J-Djdk.util.zip.disableZip64ExtraFieldValidation=true"),
      excludeTransitiveScala2
    )

def excludeTransitiveScala2 =
  excludeDependencies ++= Seq(
    ExclusionRule("org.typelevel", "cats-free_2.13"),
    ExclusionRule("dev.optics", "monocle-macro_2.13"),
    ExclusionRule("com.lihaoyi", "sourcecode_2.13"),
    ExclusionRule("com.lihaoyi", "geny_2.13"),
    ExclusionRule("org.typelevel", "simulacrum-scalafix-annotations_2.13"),
    ExclusionRule("org.typelevel:cats-kernel_2.13"),
    ExclusionRule("dev.optics", "monocle-core_2.13"),
    ExclusionRule("org.typelevel", "cats-core_2.13"),
    ExclusionRule("org.scala-lang.modules", "scala-parallel-collections_2.13"),
    ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
    ExclusionRule("org.scala-lang.modules", "scala-java8-compat_2.13"),
    ExclusionRule("com.github.pathikrit", "better-files_2.13"),


    //    ExclusionRule("org.typelevel" ,"cats_2.13"),
    //    ExclusionRule("org.typelevel" ,"cats-effect-std_2.13"),
    //    ExclusionRule("org.typelevel" ,"cats-effect_2.13"),
    //    ExclusionRule("org.typelevel", "cats-parse_2.13"),
    //    ExclusionRule("org.typelevel", "simulacrum-scalafix-annotations_2.13"),
    //    ExclusionRule("org.typelevel", "cats-kernel_2.13"),
    //    ExclusionRule("org.typelevel", "cats-effect-kernel_2.13"),
    //    ExclusionRule("org.typelevel", "cats-core_2.13")
  )

ThisBuild / publishTo :=
  (if (isSnapshot.value) Some("OpenMOLE Nexus" at "https://maven.openmole.org/snapshots") else Some("OpenMOLE Nexus" at "https://maven.openmole.org/releases"))

def scalaJSSettings = Seq(Test / fork := false)


/* ------ Third parties ---------- */

def toolDir = file("tool")
def toolSettings = scala3Settings
def allTool = Seq(
  openmoleCache,
  openmoleArchive,
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
  openmoleMath,
  openmoleTypes,
  openmoleByteCode,
  openmoleOSGi,
  openmoleRandom,
  openmoleNetwork,
  openmoleSystem,
  openmoleException,
  openmoleOutputRedirection,
  txtmark)

lazy val openmoleCache = OsgiProject(toolDir, "org.openmole.tool.cache", imports = Seq("*")) dependsOn (openmoleLogger, openmoleLock) settings (toolSettings, libraryDependencies += Libraries.squants, libraryDependencies += Libraries.cats)
lazy val openmoleArchive = OsgiProject(toolDir, "org.openmole.tool.archive", imports = Seq("*")) dependsOn (openmoleFile) settings (toolSettings, libraryDependencies += Libraries.xzJava, libraryDependencies += Libraries.compress)
lazy val openmoleDTW = OsgiProject(toolDir, "org.openmole.tool.dtw", imports = Seq("*")) settings (toolSettings)
lazy val openmoleFile = OsgiProject(toolDir, "org.openmole.tool.file", imports = Seq("*")) dependsOn(openmoleLock, openmoleStream, openmoleLogger) settings (toolSettings, libraryDependencies += Libraries.ulid)
lazy val openmoleLock = OsgiProject(toolDir, "org.openmole.tool.lock", imports = Seq("*")) dependsOn(openmoleCollection) settings (toolSettings, libraryDependencies += Libraries.gears)
lazy val openmoleLogger = OsgiProject(toolDir, "org.openmole.tool.logger", imports = Seq("*")) dependsOn (openmoleOutputRedirection) settings (toolSettings, libraryDependencies += Libraries.sourceCode)
lazy val openmoleThread = OsgiProject(toolDir, "org.openmole.tool.thread", imports = Seq("*")) dependsOn(openmoleLogger, openmoleCollection) settings (toolSettings, libraryDependencies += Libraries.squants)
lazy val openmoleHash = OsgiProject(toolDir, "org.openmole.tool.hash", imports = Seq("*")) dependsOn(openmoleFile, openmoleStream) settings (toolSettings, libraryDependencies += Libraries.codec)
lazy val openmoleStream = OsgiProject(toolDir, "org.openmole.tool.stream", imports = Seq("*")) dependsOn (openmoleThread) settings(toolSettings, libraryDependencies += Libraries.collections, libraryDependencies += Libraries.squants)
lazy val openmoleCollection = OsgiProject(toolDir, "org.openmole.tool.collection", imports = Seq("*")) settings (toolSettings *)
lazy val openmoleCrypto = OsgiProject(toolDir, "org.openmole.tool.crypto", imports = Seq("*")) settings(libraryDependencies += Libraries.bouncyCastle, libraryDependencies += Libraries.jasypt) settings (toolSettings *)
lazy val openmoleStatistics = OsgiProject(toolDir, "org.openmole.tool.statistics", imports = Seq("*")) dependsOn(openmoleLogger, openmoleTypes, openmoleDTW) settings (toolSettings *) settings (libraryDependencies += Libraries.math)
lazy val openmoleMath = OsgiProject(toolDir, "org.openmole.tool.math", imports = Seq("*")) settings (toolSettings *) settings (libraryDependencies += Libraries.math)
lazy val openmoleTypes = OsgiProject(toolDir, "org.openmole.tool.types", imports = Seq("*"), global = true) settings(libraryDependencies += Libraries.squants, Libraries.addScalaLang) settings (toolSettings *)
lazy val openmoleByteCode = OsgiProject(toolDir, "org.openmole.tool.bytecode", imports = Seq("*")) dependsOn (openmoleFile) settings (libraryDependencies += Libraries.asm) settings (toolSettings *)
lazy val openmoleOSGi = OsgiProject(toolDir, "org.openmole.tool.osgi", imports = Seq("*")) dependsOn(openmoleFile, openmoleByteCode) settings (libraryDependencies += Libraries.equinoxOSGi) settings (toolSettings *)
lazy val openmoleRandom = OsgiProject(toolDir, "org.openmole.tool.random", imports = Seq("*")) settings (toolSettings *) settings (libraryDependencies += Libraries.math) dependsOn (openmoleCache)
lazy val openmoleNetwork = OsgiProject(toolDir, "org.openmole.tool.network", imports = Seq("*")) settings (toolSettings *)
lazy val openmoleSystem = OsgiProject(toolDir, "org.openmole.tool.system", imports = Seq("*")) settings (toolSettings *) settings (libraryDependencies += Libraries.exec)
lazy val openmoleException = OsgiProject(toolDir, "org.openmole.tool.exception", imports = Seq("*")) settings(toolSettings, libraryDependencies += Libraries.squants)
lazy val openmoleOutputRedirection = OsgiProject(toolDir, "org.openmole.tool.outputredirection", imports = Seq("*")) settings (toolSettings *)

lazy val txtmark = OsgiProject(toolDir, "com.quandora.txtmark", exports = Seq("com.github.rjeschke.txtmark.*"), imports = Seq("*")) settings (toolSettings *)


/* ------------- Core ----------- */

def coreDir = file("core")
def coreProvidedScope = Osgi.openMOLEScope += "provided"
def coreSettings =
  scala3Settings ++
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
  openmoleProject,
  openmoleBuildInfo,
  module,
  market,
  context,
  openmoleArgument,
  preference,
  db,
  threadProvider,
  services,
  location,
  script,
  networkService,
  timeService,
  format,
  json,
  highlight,
  namespace,
  pluginRegistry,
  setter)

lazy val keyword = OsgiProject(coreDir, "org.openmole.core.keyword", imports = Seq("*")) settings (coreSettings *) settings(
  defaultActivator,
  libraryDependencies ++= Libraries.monocle) dependsOn (pluginRegistry)

lazy val context = OsgiProject(coreDir, "org.openmole.core.context", imports = Seq("*")) settings(
  libraryDependencies ++= Seq(Libraries.cats, Libraries.sourceCode, Libraries.shapeless),
  libraryDependencies += Libraries.fury,
  defaultActivator
) dependsOn(tools, workspace, preference, pluginRegistry) settings (coreSettings *)

lazy val setter = OsgiProject(coreDir, "org.openmole.core.setter", imports = Seq("*")) dependsOn(context, openmoleArgument, keyword) settings (
  coreSettings,
  defaultActivator
)

lazy val openmoleArgument = OsgiProject(coreDir, "org.openmole.core.argument", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.cats)
  ) dependsOn(context, tools, openmoleRandom, openmoleFile, pluginManager, openmoleCompiler, script, exception) settings (coreSettings *)

lazy val workflow = OsgiProject(coreDir, "org.openmole.core.workflow", imports = Seq("*")) settings(
  libraryDependencies ++= Seq(Libraries.math, Libraries.cats, Libraries.equinoxOSGi),
  Libraries.addScalaLang,
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
  openmoleArgument,
  threadProvider,
  script,
  networkService,
  keyword,
  format,
  json,
  openmoleBuildInfo,
  pluginRegistry,
  timeService,
  setter) settings (coreSettings *)


lazy val serializer = OsgiProject(coreDir, "org.openmole.core.serializer", global = true, imports = Seq("*")) settings(
  libraryDependencies += Libraries.xstream,
  //libraryDependencies += Libraries.fury,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(workspace, pluginManager, fileService, tools, openmoleArchive, openmoleCompiler, logconfig) settings (coreSettings)

lazy val communication = OsgiProject(coreDir, "org.openmole.core.communication", imports = Seq("*")) dependsOn(workflow, workspace) settings (coreSettings *)

lazy val openmoleDSL = OsgiProject(coreDir, "org.openmole.core.dsl", imports = Seq("*")) settings (
  libraryDependencies += Libraries.squants) dependsOn(workflow, logconfig, pluginRegistry, services) settings (coreSettings *) settings (defaultActivator)

lazy val exception = OsgiProject(coreDir, "org.openmole.core.exception", imports = Seq("*")) settings (coreSettings *)

lazy val json = OsgiProject(coreDir, "org.openmole.core.json", imports = Seq("*")) dependsOn(exception, context) settings (toolsSettings *) settings(
  libraryDependencies += Libraries.json4s,
  libraryDependencies += Libraries.jackson,
  libraryDependencies += Libraries.circe
)

lazy val format = OsgiProject(coreDir, "org.openmole.core.format", imports = Seq("*")) settings(
  coreSettings,
  OsgiKeys.bundleActivator := None,
  libraryDependencies += Libraries.circe,
  libraryDependencies += Libraries.opencsv,
  libraryDependencies += "com.volkhart.memory" % "measurer" % "0.1.1",
) dependsOn(context, json, timeService, openmoleArgument, openmoleBuildInfo)

lazy val tools = OsgiProject(coreDir, "org.openmole.core.tools", global = true, imports = Seq("*")) settings (
    coreSettings,
    libraryDependencies ++= Seq(Libraries.xstream, Libraries.exec, Libraries.math, Libraries.scalatest, Libraries.equinoxOSGi),
    Libraries.addScalaLang
  ) dependsOn(exception, openmoleException) dependsOn(allTool.map(p => p: sbt.ClasspathDep[sbt.ProjectReference]): _*)

lazy val event = OsgiProject(coreDir, "org.openmole.core.event", imports = Seq("*")) dependsOn (tools) settings (coreSettings *)

lazy val script = OsgiProject(coreDir, "org.openmole.core.script", imports = Seq("*")) dependsOn(tools, workspace) settings (coreSettings *)

lazy val replication = OsgiProject(coreDir, "org.openmole.core.replication", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.xstream, Libraries.guava)) settings (coreSettings *) dependsOn(db, preference, workspace, openmoleCache)

lazy val db = OsgiProject(coreDir, "org.openmole.core.db", imports = Seq("*")) settings (
  libraryDependencies ++= Seq(Libraries.xstream, Libraries.h2, Libraries.scopt)) settings (coreSettings *) dependsOn(openmoleNetwork, exception, openmoleCrypto, openmoleFile, openmoleLogger)

lazy val preference = OsgiProject(coreDir, "org.openmole.core.preference", imports = Seq("*")) settings(
  libraryDependencies ++= Seq(Libraries.configuration, Libraries.squants, Libraries.opencsv), Libraries.addScalaLang) settings (coreSettings *) dependsOn(openmoleNetwork, openmoleCrypto, openmoleFile, openmoleThread, openmoleTypes, openmoleLock, exception)

lazy val workspace = OsgiProject(coreDir, "org.openmole.core.workspace", imports = Seq("*")) dependsOn
  (exception, event, tools, openmoleCrypto) settings (coreSettings *)

lazy val authentication = OsgiProject(coreDir, "org.openmole.core.authentication", imports = Seq("*")) dependsOn (workspace) settings (coreSettings) settings (
  libraryDependencies += Libraries.circe
  )

lazy val services = OsgiProject(coreDir, "org.openmole.core.services", imports = Seq("*")) dependsOn(workspace, serializer, preference, fileService, networkService, threadProvider, replication, authentication, openmoleOutputRedirection, timeService) settings (coreSettings *)

lazy val location = OsgiProject(coreDir, "org.openmole.core.location", imports = Seq("*")) dependsOn (exception) settings (coreSettings *)

lazy val highlight = OsgiProject(coreDir, "org.openmole.core.highlight", imports = Seq("*")) dependsOn (exception) settings (coreSettings *)

lazy val namespace = OsgiProject(coreDir, "org.openmole.core.namespace", imports = Seq("*")) dependsOn (exception) settings (coreSettings *)

lazy val pluginManager = OsgiProject(
  coreDir,
  "org.openmole.core.pluginmanager",
  imports = Seq("*")
) settings (defaultActivator) dependsOn(exception, tools, location, openmoleOSGi) settings (coreSettings *)

lazy val pluginRegistry = OsgiProject(coreDir, "org.openmole.core.pluginregistry", imports = Seq("*")) dependsOn(exception, highlight, namespace, preference) settings (coreSettings *)


lazy val fileService = OsgiProject(coreDir, "org.openmole.core.fileservice", imports = Seq("*")) dependsOn(tools, workspace, openmoleArchive, preference, threadProvider, pluginRegistry) settings (coreSettings *) settings (defaultActivator) settings (libraryDependencies += Libraries.guava)

lazy val networkService = OsgiProject(coreDir, "org.openmole.core.networkservice", imports = Seq("*")) dependsOn(tools, workspace, preference, pluginRegistry) settings(coreSettings, libraryDependencies ++= Libraries.httpClient) settings (defaultActivator)

lazy val timeService = OsgiProject(coreDir, "org.openmole.core.timeservice", imports = Seq("*")) settings (coreSettings *)

lazy val threadProvider = OsgiProject(coreDir, "org.openmole.core.threadprovider", imports = Seq("*")) dependsOn(tools, preference, pluginRegistry) settings (coreSettings *) settings (defaultActivator)

lazy val module = OsgiProject(coreDir, "org.openmole.core.module", imports = Seq("*")) dependsOn(openmoleBuildInfo, openmoleArgument, openmoleHash, openmoleFile, pluginManager) settings(
  coreSettings,
  libraryDependencies ++= Libraries.gridscaleHTTP,
  libraryDependencies += Libraries.json4s,
  defaultActivator)

lazy val market = OsgiProject(coreDir, "org.openmole.core.market", imports = Seq("*")) enablePlugins (ScalaJSPlugin) dependsOn(openmoleBuildInfo, openmoleArgument, openmoleHash, openmoleFile, pluginManager, networkService) settings(
  coreSettings,
  libraryDependencies ++= Seq(Libraries.json4s, Libraries.circe),
  defaultActivator,
  scalaJSSettings)

lazy val logconfig = OsgiProject(
  coreDir,
  "org.openmole.core.logconfig",
  imports = Seq("*")
) settings(libraryDependencies ++= Seq(Libraries.log4j, Libraries.logback, Libraries.slf4j), defaultActivator) dependsOn (tools, openmoleOSGi) settings (coreSettings *)

lazy val outputManager = OsgiProject(coreDir, "org.openmole.core.outputmanager", imports = Seq("*")) dependsOn(openmoleStream, openmoleTypes) settings (coreSettings *) settings (defaultActivator)

lazy val openmoleCompiler = OsgiProject(coreDir, "org.openmole.core.compiler", global = true, imports = Seq("*"), exports = Seq("org.openmole.core.compiler.*", "$line5.*")) dependsOn (pluginManager) settings(
  OsgiKeys.importPackage := Seq("*"),
  Libraries.addScalaLang,
  libraryDependencies ++= Libraries.monocle,
  defaultActivator,
) dependsOn(openmoleOSGi, workspace, fileService, openmoleTypes) settings (coreSettings *)

lazy val openmoleProject = OsgiProject(coreDir, "org.openmole.core.project", imports = Seq("*")) dependsOn(namespace, openmoleCompiler, openmoleDSL, services) settings (OsgiKeys.importPackage := Seq("*")) settings (coreSettings *) settings (
  Libraries.addScalaLang)

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
        Try {
          s"figlet $banner" !!
        }.toOption.getOrElse(banner)
      }
    ),
  buildInfoPackage := s"org.openmole.core.buildinfo"
) settings (coreSettings *)


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

def allTools = Seq(netLogoAPI, netLogo5API, netLogo6API, pattern)

lazy val defaultActivator = OsgiKeys.bundleActivator := Some(name.value + ".Activator")

def pluginSettings =
  scala3Settings ++ Seq(
    defaultActivator,
    libraryDependencies += Libraries.equinoxOSGi,
    libraryDependencies += Libraries.scalatest
  )


/* Tools */

def toolsSettings = scala3Settings ++ Seq(OsgiKeys.bundleActivator := None, libraryDependencies += Libraries.scalatest)

lazy val netLogoAPI = OsgiProject(pluginDir, "org.openmole.plugin.tool.netlogo", imports = Seq("*")) settings(
  autoScalaLibrary := false,
  crossPaths := false
) settings (toolsSettings *)


lazy val netLogo5API = OsgiProject(pluginDir, "org.openmole.plugin.tool.netlogo5", imports = Seq("*")) dependsOn (netLogoAPI) settings(
  toolsSettings,
  crossPaths := false,
  autoScalaLibrary := false,
  libraryDependencies += Libraries.netlogo5,
  libraryDependencies -= Libraries.scalatest
)


lazy val netLogo6API = OsgiProject(pluginDir, "org.openmole.plugin.tool.netlogo6", imports = Seq("*")) dependsOn (netLogoAPI) settings(
  toolsSettings,
  crossPaths := false,
  autoScalaLibrary := false,
  libraryDependencies += Libraries.netlogo6,
  libraryDependencies -= Libraries.scalatest
)

lazy val pattern = OsgiProject(pluginDir, "org.openmole.plugin.tool.pattern", imports = Seq("*")) dependsOn(exception, openmoleDSL) settings (toolsSettings *) settings (defaultActivator)



/* Domain */

def allDomain = Seq(collectionDomain, distributionDomain, fileDomain, modifierDomain, rangeDomain, boundsDomain)

lazy val collectionDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.collection", imports = Seq("*")) dependsOn (openmoleDSL, modifierDomain) settings (pluginSettings *)

lazy val distributionDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.distribution", imports = Seq("*")) dependsOn (openmoleDSL, modifierDomain) settings
  (libraryDependencies ++= Seq(Libraries.math)) settings (pluginSettings *)

lazy val fileDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.file", imports = Seq("*")) dependsOn (openmoleDSL, modifierDomain) settings (pluginSettings *)

lazy val modifierDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.modifier", imports = Seq("*")) dependsOn(openmoleDSL) settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings *)

lazy val rangeDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.range", imports = Seq("*")) dependsOn (openmoleDSL, modifierDomain) settings (pluginSettings *)

lazy val boundsDomain = OsgiProject(pluginDir, "org.openmole.plugin.domain.bounds", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings *)


/* Environment */

def allEnvironment = Seq(batch, gridscale, ssh, egi, pbs, oar, sge, condor, slurm, dispatch, miniclust)

lazy val batch = OsgiProject(pluginDir, "org.openmole.plugin.environment.batch", imports = Seq("*")) dependsOn(
  workflow, workspace, tools, event, replication, exception,
  serializer, fileService, pluginManager, openmoleArchive, communication, authentication, location, services,
  openmoleByteCode, openmoleDSL
) settings (
  libraryDependencies ++= Seq(
    Libraries.gridscale,
    Libraries.h2,
    Libraries.guava,
    Libraries.jasypt),
  pluginSettings
)


//lazy val cluster = OsgiProject(pluginDir, "org.openmole.plugin.environment.cluster", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh) settings (pluginSettings *)



lazy val egi = OsgiProject(pluginDir, "org.openmole.plugin.environment.egi") dependsOn(openmoleDSL, batch, workspace, fileService, gridscale, json) settings(
  libraryDependencies ++= Libraries.gridscaleEGI, Libraries.addScalaLang) settings (pluginSettings *)

lazy val gridscale = OsgiProject(pluginDir, "org.openmole.plugin.environment.gridscale", imports = Seq("*")) settings (
  libraryDependencies += Libraries.gridscaleLocal) dependsOn(openmoleDSL, tools, batch, exception) settings (pluginSettings *)

lazy val oar = OsgiProject(pluginDir, "org.openmole.plugin.environment.oar", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh, dispatch) settings
  (libraryDependencies += Libraries.gridscaleOAR) settings (pluginSettings *)

lazy val pbs = OsgiProject(pluginDir, "org.openmole.plugin.environment.pbs", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh, dispatch) settings
  (libraryDependencies += Libraries.gridscalePBS) settings (pluginSettings *)

lazy val sge = OsgiProject(pluginDir, "org.openmole.plugin.environment.sge", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh, dispatch) settings
  (libraryDependencies += Libraries.gridscaleSGE) settings (pluginSettings *)

lazy val condor = OsgiProject(pluginDir, "org.openmole.plugin.environment.condor", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh, dispatch) settings
  (libraryDependencies += Libraries.gridscaleCondor) settings (pluginSettings *)

lazy val slurm = OsgiProject(pluginDir, "org.openmole.plugin.environment.slurm", imports = Seq("*")) dependsOn(openmoleDSL, batch, gridscale, ssh, dispatch) settings
  (libraryDependencies += Libraries.gridscaleSLURM) settings (pluginSettings *)

lazy val ssh = OsgiProject(pluginDir, "org.openmole.plugin.environment.ssh", imports = Seq("*")) dependsOn(openmoleDSL, event, batch, gridscale, json, dispatch) settings
  (libraryDependencies ++= Libraries.gridscaleSSH) settings (pluginSettings *)

lazy val miniclust = OsgiProject(pluginDir, "org.openmole.plugin.environment.miniclust", imports = Seq("*")) dependsOn(openmoleDSL, event, batch, gridscale) settings
  (libraryDependencies += Libraries.gridscaleMiniclust, pluginSettings)


lazy val dispatch = OsgiProject(pluginDir, "org.openmole.plugin.environment.dispatch", imports = Seq("*")) dependsOn(openmoleDSL, event, batch, gridscale) settings (pluginSettings *)

/* Hook */

def allHook = Seq(displayHook, fileHook, modifierHook)

lazy val displayHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.display", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings *)

lazy val fileHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.file", imports = Seq("*")) dependsOn(openmoleDSL, replication % "test") settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings *)

lazy val modifierHook = OsgiProject(pluginDir, "org.openmole.plugin.hook.modifier", imports = Seq("*")) dependsOn (openmoleDSL) settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings *)


/* Method */

def allMethod = Seq(evolution, directSampling, sensitivity, abc)

lazy val evolution = OsgiProject(pluginDir, "org.openmole.plugin.method.evolution", imports = Seq("*"), excludeSubPackage = Seq("data")) dependsOn(
  openmoleDSL, toolsTask, pattern, scalaTask, collectionDomain % "test", boundsDomain % "test"
) settings(
  libraryDependencies += Libraries.mgo,
  libraryDependencies += Libraries.circe,
  excludeDependencies += ExclusionRule(organization = "org.typelevel", name = "cats-kernel_2.13")
) settings (pluginSettings *)

lazy val abc = OsgiProject(pluginDir, "org.openmole.plugin.method.abc", imports = Seq("*")) dependsOn(openmoleDSL, toolsTask, pattern, boundsDomain % "test") settings (
  libraryDependencies += Libraries.mgo) settings (pluginSettings *)

lazy val directSampling = OsgiProject(pluginDir, "org.openmole.plugin.method.directsampling", imports = Seq("*")) dependsOn(openmoleDSL, distributionDomain, pattern, modifierDomain, fileHook, combineSampling, scalaTask) settings (pluginSettings *)

lazy val sensitivity = OsgiProject(pluginDir, "org.openmole.plugin.method.sensitivity", imports = Seq("*")) dependsOn(exception, workflow, workspace, openmoleDSL, lhsSampling, quasirandomSampling, directSampling, collectionDomain % "test", boundsDomain % "test") settings (pluginSettings *)


/* Sampling */

def allSampling = Seq(combineSampling, csvSampling, oneFactorSampling, lhsSampling, quasirandomSampling)

lazy val combineSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.combine", imports = Seq("*")) dependsOn(exception, modifierDomain, collectionDomain, workflow) settings (pluginSettings *)

lazy val csvSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.csv", imports = Seq("*")) dependsOn(exception, workflow, openmoleDSL) settings (
  libraryDependencies += Libraries.scalatest
  ) settings (pluginSettings)

lazy val oneFactorSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.onefactor", imports = Seq("*")) dependsOn(exception, workflow, openmoleDSL, combineSampling, collectionDomain) settings (pluginSettings *)

lazy val lhsSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.lhs", imports = Seq("*")) dependsOn(exception, workflow, workspace, openmoleDSL) settings (pluginSettings *)

lazy val quasirandomSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.quasirandom", imports = Seq("*")) dependsOn(exception, workflow, workspace, openmoleDSL) settings (
  libraryDependencies += Libraries.math
  ) settings (pluginSettings *)


/* Source */

def allSource = Seq(fileSource, httpURLSource)

lazy val fileSource = OsgiProject(pluginDir, "org.openmole.plugin.source.file", imports = Seq("*")) dependsOn(openmoleDSL, serializer, exception) settings (pluginSettings *)

lazy val httpURLSource = OsgiProject(pluginDir, "org.openmole.plugin.source.httpurl", imports = Seq("*")) dependsOn(openmoleDSL, exception, networkService) settings (pluginSettings *)


/* Task */

def allTask = Seq(toolsTask, externalTask, netLogoTask, netLogo5Task, netLogo6Task, javaTask, scalaTask, templateTask, systemexecTask, containerTask, rTask, scilabTask, pythonTask, juliaTask, gamaTask, cormasTask, spatialTask, timingTask)

lazy val toolsTask = OsgiProject(pluginDir, "org.openmole.plugin.task.tools", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings *)

lazy val externalTask = OsgiProject(pluginDir, "org.openmole.plugin.task.external", imports = Seq("*")) dependsOn(openmoleDSL, workspace) settings (pluginSettings *)

// Because NetLogo bundle contains scala classes
def noNetLogoInClassPath =
  Compile / dependencyClasspath := (Compile / dependencyClasspath).value.filter(!_.data.name.contains("ccl-northwestern-edu-netlogo"))

lazy val netLogoTask = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo", imports = Seq("*")) dependsOn(openmoleDSL, externalTask, netLogoAPI, containerTask) settings(
  pluginSettings,
  libraryDependencies += Libraries.scalaXML)

lazy val netLogo5Task = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo5") dependsOn(netLogoTask, openmoleDSL, externalTask, netLogo5API) settings (pluginSettings *) settings(
  noNetLogoInClassPath,
  libraryDependencies += Libraries.netlogo5)

lazy val netLogo6Task = OsgiProject(pluginDir, "org.openmole.plugin.task.netlogo6", imports = Seq("*")) dependsOn(netLogoTask, openmoleDSL, externalTask, netLogo6API) settings (pluginSettings *) settings(
  noNetLogoInClassPath,
  libraryDependencies += Libraries.netlogo6)

lazy val scalaTask = OsgiProject(pluginDir, "org.openmole.plugin.task.scala", imports = Seq("*")) dependsOn(openmoleDSL, externalTask, openmoleCompiler) settings (pluginSettings *) settings (
  libraryDependencies += Libraries.scalaXML
  )

lazy val templateTask = OsgiProject(pluginDir, "org.openmole.plugin.task.template", imports = Seq("*")) dependsOn(openmoleDSL, replication % "test") settings (
  libraryDependencies += Libraries.scalatest) settings (pluginSettings *)

lazy val systemexecTask = OsgiProject(pluginDir, "org.openmole.plugin.task.systemexec", imports = Seq("*")) dependsOn(openmoleDSL, externalTask, workspace) settings (
  libraryDependencies += Libraries.exec) settings (pluginSettings *)

lazy val containerTask = OsgiProject(pluginDir, "org.openmole.plugin.task.container", imports = Seq("*")) dependsOn(openmoleFile, pluginManager, externalTask, openmoleArgument, exception) settings (pluginSettings *) settings (
  libraryDependencies += Libraries.container)

lazy val rTask = OsgiProject(pluginDir, "org.openmole.plugin.task.r", imports = Seq("*")) dependsOn(tools, containerTask, json) settings (
  libraryDependencies ++= Libraries.httpClient
  ) settings (pluginSettings *)

lazy val javaTask = OsgiProject(pluginDir, "org.openmole.plugin.task.java", imports = Seq("*")) dependsOn(containerTask, json) settings (pluginSettings *)

lazy val scilabTask = OsgiProject(pluginDir, "org.openmole.plugin.task.scilab", imports = Seq("*")) dependsOn (containerTask) settings (pluginSettings *)

lazy val pythonTask = OsgiProject(pluginDir, "org.openmole.plugin.task.python", imports = Seq("*")) dependsOn(containerTask, json) settings (pluginSettings *)

lazy val juliaTask = OsgiProject(pluginDir, "org.openmole.plugin.task.julia", imports = Seq("*")) dependsOn(containerTask, json) settings (pluginSettings *)

lazy val gamaTask = OsgiProject(pluginDir, "org.openmole.plugin.task.gama", imports = Seq("*")) dependsOn (containerTask) settings (pluginSettings *) settings (
  libraryDependencies += Libraries.scalaXML
  )

lazy val cormasTask = OsgiProject(pluginDir, "org.openmole.plugin.task.cormas", imports = Seq("*")) dependsOn(containerTask, json) settings (pluginSettings *) settings (
  libraryDependencies += Libraries.json4s)

lazy val timingTask = OsgiProject(pluginDir, "org.openmole.plugin.task.timing", imports = Seq("*")) dependsOn (openmoleDSL) settings (pluginSettings *)

lazy val spatialTask = OsgiProject(pluginDir, "org.openmole.plugin.task.spatial", imports = Seq("*")) dependsOn (openmoleDSL) settings(
  libraryDependencies += Libraries.math,
  libraryDependencies += Libraries.spatialsampling
) settings (pluginSettings *)

/* ---------------- REST ------------------- */


def restDir = file("rest")

lazy val message = OsgiProject(restDir, "org.openmole.rest.message") settings(
  scala3Settings,
  libraryDependencies += Libraries.circe)

lazy val server = OsgiProject(
  restDir,
  "org.openmole.rest.server",
  imports = Seq("org.h2", "!com.sun.*", "*")
) dependsOn(workflow, openmoleArchive, openmoleCollection, openmoleProject, message, openmoleCrypto, services, module, serverExt) settings(
  scala3Settings,
  libraryDependencies ++= Seq(Libraries.logback, Libraries.codec, Libraries.http4s, Libraries.cats),
  Libraries.addScalaLang)


/* -------------------- GUI --------------------- */


def guiDir = file("gui")

def guiSharedDir = guiDir / "shared"
def guiSettings = scala3Settings
//def guiSettings3 = defaultSettings ++ excludeTransitiveScala2

def guiStrictImports = Seq("!org.scalajs.*", "!com.raquo.*", "!scala.scalajs.*", "!scaladget.*", "!org.openmole.plotlyjs.*", "!org.querki.*", "*")

/* -------------------- GUI Client ----------------------*/

val clientPrivatePackages = Seq("com.raquo.*", "org.scalajs.dom.*", "scaladget.*", "net.scalapro.sortable.*", "org.openmole.plotlyjs.*", "org.querki.jsext.*", "app.tulz.tuplez.*")

def guiClientDir = guiDir / "client"
lazy val clientGUI = OsgiProject(guiClientDir, "org.openmole.gui.client.core") enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin) settings(
  //lazy val clientGUI = OsgiProject(guiClientDir, "org.openmole.gui.client.core") dependsOn (apiGUI, clientToolGUI, market, dataGUI, clientExt) settings (
  libraryDependencies += Libraries.sourceCode,
  webpackBundlingMode := BundlingMode.LibraryAndApplication(),
  webpackNodeArgs := Seq("--openssl-legacy-provider"),
  webpack / version := Libraries.wepackVersion,
  // Compile / npmDeps += Dep("ace-builds/src-min", "1.4.3", List("mode-scala.js", "theme-github.js", "ext-language_tools.js"), true),
  // Compile / npmDeps += Dep("sortablejs", "1.10.2", List("Sortable.min.js"))
  guiSettings,
  test := false
) dependsOn(apiGUI, clientToolGUI, market, dataGUI, clientExt)


lazy val clientToolGUI = OsgiProject(guiClientDir, "org.openmole.gui.client.tool", privatePackages = clientPrivatePackages) enablePlugins (ScalaJSPlugin) settings(
  guiSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.scalajs,
  //  Libraries.autowireJS,
  //  Libraries.boopickleJS,
  Libraries.scalajsDomJS,
  Libraries.ace,
  Libraries.bootstrapnative,
  Libraries.nouiSlider,
  Libraries.scaladgetTools,
  Libraries.laminarJS,
  //Libraries.endpoints4SJS,
  //Libraries.catsJS,
  // Libraries.sortable,
  Libraries.plotlyJS) dependsOn (clientExt)

lazy val clientExt = OsgiProject(guiClientDir, "org.openmole.gui.client.ext") enablePlugins (ScalaJSPlugin) dependsOn(dataGUI, apiGUI) settings(
  //  Libraries.boopickleJS,
  //  Libraries.autowireJS,
  Libraries.laminarJS,
  Libraries.scalajsDomJS,
  Libraries.scaladgetTools,
  Libraries.bootstrapnative,
  libraryDependencies += Libraries.endpoints4s,
  guiSettings,
  scalaJSSettings)


val build = taskKey[Unit]("build")


lazy val clientStub = Project("org-openmole-gui-client-stub", guiClientDir / "org.openmole.gui.client.stub") enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin) settings(
  //version := ope,
  //scalaVersion := ScalaVersion,
  //crossScalaVersions := supportedVersion,
  name := "org-openmole-gui-client-stub",
  scalaJSUseMainModuleInitializer := false,
  webpackBundlingMode := BundlingMode.LibraryAndApplication(),
  webpackNodeArgs := Seq("--openssl-legacy-provider"),
  //webpackExtraArgs := Seq("--profile", "--progress", "true"),
  webpackEmitSourceMaps := false,
  test := false,
  /*libraryDependencies ++= Seq(
    "com.raquo" %%% "laminar" % laminarVersion,
    "org.openmole.scaladget" %%% "tools" % scaladgetVersion,
    "org.openmole.scaladget" %%% "svg" % scaladgetVersion,
    "org.openmole.scaladget" %%% "bootstrapnative" % scaladgetVersion,
    "org.scala-js" %%% "scalajs-dom" % scalajsDomVersion,
    "org.openmole.endpoints4s" %%% "xhr-client" % "5.1.0+n"
  )*/
  guiSettings,
  scalaJSSettings,
  build := {

    val demoResource = (Compile / resourceDirectory).value
    val demoTarget = target.value
    val bundlerTarget = demoTarget / ("scala-" + scalaVersion.value) / "scalajs-bundler"

    IO.copyFile(demoResource / "webapp/js/openmole_grammar_stub.js", bundlerTarget / "main" / "node_modules" / "ace-builds" / "src-noconflict" / "mode-openmole.js")

    val jsBuilt = (Compile / fastOptJS / webpack).value
    val jsBuild = jsBuilt.head.data

    //jsBuilt.last.data.name

    //val sourceMap = scalaJSSourceMap.value

    IO.copyDirectory(demoResource, demoTarget)
    IO.copyFile(jsBuild, demoTarget / "webapp/js/openmole-webpacked.js")
    //IO.copyFile(scalaJSSourceMap., demoTarget / "webapp/js/openmole-webpacked.js.map")

    IO.copyFile(bundlerTarget / "main" / "node_modules" / "ace-builds" / "src-min-noconflict" / "ace.js", demoTarget / "webapp" / "js" / "ace.js")
    IO.copyFile(bundlerTarget / "main" / "node_modules" / "plotly.js" / "dist" / "plotly.min.js", demoTarget / "webapp" / "js" / "plotly.min.js")
    IO.copyFile(demoResource / "webapp/js/openmole_grammar_stub.js", demoTarget / "webapp" / "js" / "mode-openmole.js")

    (Compile / compile).value
  }
) dependsOn(clientGUI, guiEnvironmentSSHLoginPlugin)


/* -------------------------- GUI Server ----------------------- */

def guiServerDir = guiDir / "server"

lazy val serverGUI = OsgiProject(guiServerDir, "org.openmole.gui.server.core", dynamicImports = Seq("org.eclipse.jetty.*")) settings(
  libraryDependencies ++= Seq(Libraries.scalaTags, Libraries.endpoints4s, Libraries.http4s, Libraries.cats, Libraries.scalaXML, Libraries.sshj),
  guiSettings) dependsOn(
  apiGUI,
  dataGUI,
  workflow,
  openmoleBuildInfo,
  openmoleFile,
  openmoleArchive,
  openmoleHash,
  openmoleCollection,
  openmoleProject,
  openmoleDSL,
  batch,
  containerTask,
  openmoleStream,
  txtmark,
  openmoleCrypto,
  module,
  market,
  serverExt,
  jsCompile,
  services,
  location,
  serverGit)

lazy val serverExt = OsgiProject(guiServerDir, "org.openmole.gui.server.ext") dependsOn(apiGUI, workspace, module, services, serverGit) settings(
  libraryDependencies += Libraries.equinoxOSGi,
  libraryDependencies ++= Seq(Libraries.endpoints4s, Libraries.http4s, Libraries.cats, Libraries.jgit, Libraries.sshj),
  guiSettings,
  scalaJSSettings)


lazy val serverGit = OsgiProject(guiServerDir, "org.openmole.gui.server.git") dependsOn(apiGUI) settings(
  libraryDependencies += Libraries.equinoxOSGi,
  libraryDependencies ++= Seq(Libraries.jgit),
  guiSettings,
  scalaJSSettings)


lazy val jsCompile = OsgiProject(guiServerDir, "org.openmole.gui.server.jscompile", imports = Seq("*")) dependsOn(pluginManager, fileService, workspace, networkService, dataGUI) settings(
  guiSettings,
  libraryDependencies += "org.scala-js" %%% "scalajs-library" % scalajsVersion % "provided" intransitive() cross CrossVersion.for3Use2_13,
  //libraryDependencies += "org.scala-lang.modules" %%% "scala-collection-compat" % "2.1.4" % "provided" intransitive(),

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
  }
)

lazy val serverStub = Project("org-openmole-gui-server-stub", guiServerDir / "org.openmole.gui.server.stub") settings(
  guiSettings,
  libraryDependencies ++= Seq(Libraries.endpoints4s, Libraries.http4s),
  Compile / run / fork := true,
  Compile / run / connectInput := true,
  Compile / run / outputStrategy := Some(StdoutOutput),

  test := false,

  Compile / compile := {
    val targetModules = target.value / "node_modules"

    IO.withTemporaryDirectory { modules =>
      import sys.process._

      val packageJson = (serverGUI / Compile / resourceDirectory).value / "webpack/package.json"
      val cacheFile = target.value / "node_modules-hash"
      val packageJsonHash = Hash.toHex(FileInfo.hash(packageJson).hash.toArray)

      if (!cacheFile.exists || IO.read(cacheFile) != packageJsonHash) {
        IO.copyFile(packageJson, modules / "package.json")
        Process("npm install", Some(modules)).!!
        IO.copyDirectory(modules, targetModules)
        IO.delete(modules)
        IO.write(cacheFile, packageJsonHash)
      }
    }

    //val jsBuild = (clientGUI / Compile / fastOptJS / webpack).value.head.data

    //    val bundlerTarget = (clientStub / target).value / ("scala-" + scalaVersion.value) / "scalajs-bundler"
    //

    IO.copyDirectory((clientStub / Compile / resourceDirectory).value / "webapp", target.value / "webapp")
    val jsBuild = (clientStub / Compile / fastOptJS / webpack).value.head.data
    IO.copyFile(jsBuild, target.value / "webapp/js/openmole-webpacked.js")
    //    //IO.copyFile(new File(jsBuild.toString + ".map"), target.value / ("webapp/js/" + jsBuild.getName + ".map"))
    //
    IO.copyFile(targetModules / "node_modules" / "ace-builds" / "src-min-noconflict" / "ace.js", target.value / "webapp" / "js" / "ace.js")
    IO.copyFile(targetModules / "node_modules" / "ace-builds" / "src-min-noconflict" / "snippets/text.js", target.value / "webapp" / "js" / "text.js")
    IO.copyFile(targetModules / "node_modules" / "plotly.js" / "dist" / "plotly.min.js", target.value / "webapp" / "js" / "plotly.min.js")
    IO.copyFile(targetModules / "node_modules" / "nouislider" / "dist" / "nouislider.min.js", target.value / "webapp" / "js" / "nouislider.min.js")
    IO.copyDirectory(targetModules / "node_modules" / "bootstrap-icons" / "font", target.value / "webapp" / "css" / "bootstrap-icons")

    IO.copyFile((clientStub / Compile / resourceDirectory).value / "webapp/js/openmole_grammar_stub.js", target.value / "webapp" / "js" / "mode-openmole.js")

    (Compile / compile).value
  },
  run := Def.taskDyn {
    (Compile / run).toTask(" " + (Compile / target).value + "/webapp")
  }.value,

) dependsOn(apiGUI, serverGUI)


/* -------------------- GUI Shared ----------------------*/

lazy val dataGUI = OsgiProject(guiSharedDir, "org.openmole.gui.shared.data", imports = guiStrictImports) enablePlugins (ScalaJSPlugin) settings(
  Libraries.scalajsDomJS,
  Libraries.laminarJS,
  libraryDependencies += Libraries.endpoints4s,
  guiSettings,
  scalaJSSettings) dependsOn (format)

lazy val apiGUI = OsgiProject(guiSharedDir, "org.openmole.gui.shared.api", imports = guiStrictImports /*dynamicImports = Seq("shapeless.*", "endpoints4s.generic.*", "endpoints4s.algebra.*")*/) dependsOn(dataGUI, market) enablePlugins (ScalaJSPlugin) settings (guiSettings) settings(
  //libraryDependencies += Libraries.endpoint4SJsonSchemaGeneric,
  libraryDependencies += Libraries.endpoints4s,
  scalaJSSettings
)

/* -------------------- GUI Plugin ----------------------- */

def guiPluginSettings = guiSettings ++ Seq(defaultActivator)

def guiPluginDir = guiDir / "plugins"

def guiPlugins = Seq(
  guiEnvironmentSSHLoginPlugin,
  guiEnvironmentSSHKeyPlugin,
  guiEnvironmentEGIPlugin,
  guiEnvironmentMiniclustPlugin,
  netlogoWizardPlugin,
  gamaWizardPlugin,
  rWizardPlugin,
  javaWizardPlugin,
  juliaWizardPlugin,
  pythonWizardPlugin,
  scilabWizardPlugin,
  containerWizardPlugin,
  evolutionAnalysisPlugin
  // Obsolete
  //nativeWizardPlugin,
  // jarWizardPlugin,
) //, guiEnvironmentDesktopGridPlugin)

lazy val guiEnvironmentEGIPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.egi", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  Libraries.bootstrapnative,
  excludeDependencies += ExclusionRule("org.scala-lang.modules", "scala-xml_3")
) dependsOn(serverExt, clientExt, apiGUI, workspace, egi) enablePlugins (ScalaJSPlugin)

lazy val guiEnvironmentSSHKeyPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.sshkey", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  Libraries.bootstrapnative
) dependsOn(serverExt, clientExt, apiGUI, workspace, ssh) enablePlugins (ScalaJSPlugin)

lazy val guiEnvironmentSSHLoginPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.sshlogin", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  Libraries.bootstrapnative
) dependsOn(serverExt, clientExt, apiGUI, workspace, ssh) enablePlugins (ScalaJSPlugin)

lazy val guiEnvironmentMiniclustPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.authentication.miniclust", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  Libraries.bootstrapnative
) dependsOn(serverExt, clientExt, apiGUI, workspace, miniclust) enablePlugins (ScalaJSPlugin)

lazy val netlogoWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.netlogo", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(serverExt, clientExt, serverExt, workspace) enablePlugins (ScalaJSPlugin)

//lazy val nativeWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.native") settings(
//  guiPluginSettings,
//  libraryDependencies += Libraries.equinoxOSGi,
//  libraryDependencies += Libraries.arm
//) dependsOn(extServer, clientExt, extServer, workspace) enablePlugins (ScalaJSPlugin)

lazy val gamaWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.gama", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(serverExt, clientExt, serverExt, workspace) enablePlugins (ScalaJSPlugin)

lazy val juliaWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.julia", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(serverExt, clientExt, serverExt, workspace) enablePlugins (ScalaJSPlugin)


lazy val pythonWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.python", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(serverExt, clientExt, serverExt, workspace) enablePlugins (ScalaJSPlugin)


lazy val scilabWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.scilab", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(serverExt, clientExt, serverExt, workspace) enablePlugins (ScalaJSPlugin)


lazy val rWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.r", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(serverExt, clientExt, serverExt, workspace) enablePlugins (ScalaJSPlugin)


lazy val javaWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.java", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(serverExt, clientExt, serverExt, workspace) enablePlugins (ScalaJSPlugin)


lazy val containerWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.container", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi
) dependsOn(serverExt, clientExt, serverExt, workspace) enablePlugins (ScalaJSPlugin)

//lazy val jarWizardPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.wizard.jar") settings(
//  guiPluginSettings,
//  libraryDependencies += Libraries.equinoxOSGi,
//) dependsOn(extServer, clientExt, extServer, workspace) enablePlugins (ScalaJSPlugin)

lazy val evolutionAnalysisPlugin = OsgiProject(guiPluginDir, "org.openmole.gui.plugin.analysis.evolution", imports = guiStrictImports) settings(
  guiPluginSettings,
  scalaJSSettings,
  libraryDependencies += Libraries.equinoxOSGi,
  Libraries.plotlyJS
) dependsOn(serverExt, clientExt, serverExt, workspace, evolution) enablePlugins (ScalaJSPlugin)



/* -------------------- Bin ------------------------- */

def binDir = file("bin")


def bundleFilter(m: ModuleID, artifact: Artifact) = {
  //def excludedLibraryDependencies = Set("slick", "squants", "shapeless", "sourcecode", "eddsa", "sshj")

  def exclude = false
  /*    (m.organization != "org.openmole.library" && excludedLibraryDependencies.exists(m.name.contains)) ||
        (m.name contains "scala-xml") ||
        (m.name contains "protobuf") ||
        (m.name contains "jts-core") || (m.name contains "si-quantity") || (m.name contains "systems-common-java8") || (m.name contains "uom-lib-common") || (m.name contains "unit-api") || (m.name contains "uom-se") // geotools bundled dependancies */

  def includeOtherBundles =
    Set[(String, String)](
      ("", "scala-parser-combinators"),
      // akka http stream
      //      ("", "akka-stream"),
      //      ("", "akka-actor"),
      //      ("com.typesafe", "config")
    )

  def include =
    (artifact.`type` == "bundle" && m.name != "osgi" && m.organization == "org.openmole.library") ||
      (m.organization == "org.bouncycastle" && !m.name.contains("jdk15on")) ||
      (m.name == "httpclient-osgi") ||
      (m.name == "httpcore-osgi") ||
      (m.organization == "org.osgi" && m.name != "osgi") ||
      includeOtherBundles.exists { case (org, name) => m.organization.contains(org) && m.name.contains(name) }

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
) settings (scala3Settings *) settings (excludeTransitiveScala2)

def minimumPlugins =
  Seq(
    collectionDomain,
    distributionDomain,
    fileDomain,
    modifierDomain,
    rangeDomain,
    combineSampling,
    scalaTask
  )

def openmoleNakedDependencies = allCore ++ Seq(openmoleUI) ++ minimumPlugins

def openmoleDependencies = openmoleNakedDependencies ++ corePlugins ++ guiPlugins

def requieredRuntimeLibraries = Seq(Libraries.osgiCompendium, Libraries.logging)

lazy val openmoleNaked =
  Project("openmole-naked", binDir / "openmole-naked") settings (assemblySettings) enablePlugins (ScalaJSPlugin) settings(
    setExecutable ++= Seq("openmole", "openmole.bat"),
    Compile / Osgi.bundleDependencies := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject, includeRoot = false))).value,
    resourcesAssemble += (Compile / resourceDirectory).value -> assemblyPath.value,
    resourcesAssemble += ((serverGUI / Compile / resourceDirectory).value / "webapp") → (assemblyPath.value / "webapp"),
    resourcesAssemble += ((serverGUI / Compile / resourceDirectory).value / "webpack") → (assemblyPath.value / "webpack"),
    //  IO.copyFile(crossTarget.value / s"${name.value}-jsdeps.js", (Compile / resourceManaged).value / "deps.js"),

    //FIXME
    //resourcesAssemble += ((clientGUI / crossTarget).value / s"${name.value}-jsdeps.js") -> (assemblyPath.value / "webapp" / "deps.js"),


    //   resourcesAssemble += (clientGUI / Compile / dependencyFile).value -> (assemblyPath.value / "webapp/js/deps.js"),
    // resourcesAssemble += (clientGUI / Compile / cssFile).value -> (assemblyPath.value / "webapp/css/"),
    resourcesAssemble += {
      val tarFile = (openmoleRuntime / tar).value
      tarFile -> (assemblyPath.value / "runtime" / tarFile.getName)
    },
    resourcesAssemble += (launcher / assemble).value -> (assemblyPath.value / "launcher"),
    resourcesAssemble ++= (Compile / Osgi.bundleDependencies).value.map(b => b → (assemblyPath.value / "plugins" / b.getName)),
    resourcesAssemble += {
      IO.withTemporaryDirectory { modules =>
        import sys.process._

        val packageJson = (serverGUI / Compile / resourceDirectory).value / "webpack/package.json"
        val cacheFile = target.value / "node_modules-hash"
        val zip = target.value / "node_modules.zip"
        val packageJsonHash = Hash.toHex(FileInfo.hash(packageJson).hash.toArray)

        if (!zip.exists || !cacheFile.exists || IO.read(cacheFile) != packageJsonHash) {
          IO.copyFile(packageJson, modules / "package.json")
          Process("npm install", Some(modules)) !!

          def content = {
            import _root_.java.nio.file.*
            import _root_.scala.collection.JavaConverters.*

            val path = modules.toPath
            Files.walk(path).iterator.asScala.map(f => f.toFile -> path.relativize(f).toString).toTraversable
            //sbt.Path.contentOf(modules)
          }

          IO.zip(content, zip, None)
          IO.write(cacheFile, packageJsonHash)
        }

        zip -> (assemblyPath.value / "webpack" / "node_modules.zip")
      }
    },

    libraryDependencies ++= requieredRuntimeLibraries,
    dependencyFilter := bundleFilter,
    dependencyName := rename,
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    tarName := "openmole-naked.tar.gz",
    tarInnerFolder := "openmole",
    cleanFiles ++= (launcher / cleanFiles).value,
    cleanFiles ++= (openmoleRuntime / cleanFiles).value,
    scala3Settings,
    excludeTransitiveScala2,
    test := false
  ) dependsOn (toDependencies(openmoleNakedDependencies) *)

lazy val openmole =
  Project("openmole", binDir / "openmole") enablePlugins (TarPlugin) settings (assemblySettings) settings (scala3Settings) settings(
    setExecutable ++= Seq("openmole", "openmole.bat"),
    Compile / Osgi.bundleDependencies := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject, includeRoot = false))).value,
    tarName := "openmole.tar.gz",
    tarInnerFolder := "openmole",
    dependencyFilter := bundleFilter,
    dependencyName := rename,
    resourcesAssemble += (openmoleNaked / assemble).value -> assemblyPath.value,
    resourcesAssemble ++= (Compile / Osgi.bundleDependencies).value.map(b => b → (assemblyPath.value / "plugins" / b.getName)),
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    cleanFiles ++= (openmoleNaked / cleanFiles).value
  ) dependsOn (toDependencies(openmoleDependencies) *) settings (excludeTransitiveScala2)

lazy val openmoleRuntime =
  OsgiProject(binDir, "org.openmole.runtime", singleton = true, imports = Seq("*")) enablePlugins (TarPlugin) settings (assemblySettings *) dependsOn(workflow, communication, serializer, logconfig, event, exception) settings(
    assemblyDependenciesPath := assemblyPath.value / "plugins",
    resourcesAssemble += (Compile / resourceDirectory).value -> assemblyPath.value,
    resourcesAssemble += (launcher / assemble).value -> (assemblyPath.value / "launcher"),
    resourcesAssemble ++= (Compile / Osgi.bundleDependencies).value.map(b => b → (assemblyPath.value / "plugins" / b.getName)),
    setExecutable ++= Seq("run.sh"),
    tarName := "runtime.tar.gz",
    libraryDependencies ++= requieredRuntimeLibraries,
    libraryDependencies += Libraries.scopt,
    dependencyFilter := bundleFilter,
    dependencyName := rename
  ) dependsOn (toDependencies(allCore) *) settings (scala3Settings *)


lazy val api = Project("api", binDir / "target" / "api") settings (scala3Settings *) enablePlugins (ScalaUnidocPlugin) settings (
  //compile := sbt.inc.Analysis.Empty,
  ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(openmoleDependencies.map(p => p: ProjectReference) *)
  // -- inProjects(Libraries.projects.map(p => p: ProjectReference) ++ ThirdParties.projects.map(p => p: ProjectReference)*/
  //  Tar.name := "openmole-api.tar.gz",
  //  Tar.folder := (UnidocKeys.unidoc in Compile).map(_.head).value
  )

lazy val site = crossProject(JSPlatform, JVMPlatform).in(binDir / "org.openmole.site")

lazy val siteJS = site.js enablePlugins (ScalaJSBundlerPlugin) settings(
  webpackBundlingMode := BundlingMode.LibraryAndApplication(),
  scalaJSLinkerConfig := scalaJSLinkerConfig.value.withSourceMap(true),
  webpackNodeArgs := Seq("--openssl-legacy-provider"),
  webpack / version := Libraries.wepackVersion,
  scala3Settings,
  test := {},
  Libraries.laminarJS,
  Libraries.bootstrapnative,
  Libraries.lunr,
  Libraries.scaladgetTools,
  Libraries.scalajsDomJS,
  Libraries.highlightJS,
  libraryDependencies += Libraries.scalaTags,
)

lazy val siteJVM = site.jvm dependsOn(tools, openmoleProject, serializer, openmoleBuildInfo, marketIndex) settings(
  libraryDependencies += "com.lihaoyi" %% "sourcecode" % sourcecodeVersion,
  //libraryDependencies +=  "org.json4s" %% "json4s-jackson" % json4sVersion,
  //  libraryDependencies += Libraries.json4s,
  libraryDependencies += Libraries.spray,
  //  libraryDependencies += Libraries.txtmark,
  libraryDependencies += Libraries.scalaTags,
  //libraryDependencies += Libraries.scalajHttp,
  libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % scalaXMLVersion,

  //  excludeDependencies ++= Seq(
  //    ExclusionRule("com.lihaoyi", "sourcecode_3"),
  //    ExclusionRule("com.lihaoyi", "geny_3"),
  //    ExclusionRule("com.lihaoyi", "scalatags_2.13"),
  //    //ExclusionRule("org.typelevel", "cats-kernel_3"),
  //    //ExclusionRule("org.typelevel", "cats-core_3"),
  //    //ExclusionRule("org.scala-lang.modules", "scala-xml_3")
  //  )
  //libraryDependencies ~= _.map(_ excludeAll (ExclusionRule(organization = "com.lihaoyi", name = "sourcecode_2.13"))),
)

lazy val cloneMarket = taskKey[Unit]("cloning market place")
lazy val defineMarketBranch = taskKey[Option[String]]("define market place branch")

lazy val marketIndex = Project("marketindex", binDir / "org.openmole.marketindex") settings (scala3Settings *) settings(
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
) dependsOn(openmoleBuildInfo, openmoleFile, openmoleArchive, market)

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

  def copySiteResources(siteBuildJS: File, dependencyFile: Option[File], resourceDirectory: File, siteTarget: File, cssFile: Option[File]) = {
    IO.copyFile(siteBuildJS, siteTarget / "js/sitejs.js")
    // dependencyFile.foreach(d => IO.copyFile(d, siteTarget / "js/deps.js"))
    IO.copyDirectory(resourceDirectory / "js", siteTarget / "js")
    IO.copyDirectory(resourceDirectory / "css", siteTarget / "css")
    cssFile.foreach(d => IO.copyDirectory(d, siteTarget / "css"))
    IO.copyDirectory(resourceDirectory / "fonts", siteTarget / "fonts")
    IO.copyDirectory(resourceDirectory / "img", siteTarget / "img")
    IO.copyDirectory(resourceDirectory / "bibtex", siteTarget / "bibtex")
    IO.copyDirectory(resourceDirectory / "script", siteTarget / "script")
    IO.copyDirectory(resourceDirectory / "paper", siteTarget / "paper")
  }

  copySiteResources((siteJS / Compile / fastOptJS / webpack).value.head.data,
    // copySiteResources((siteJS / Compile / fullOptJS).value.data,
    None,
    //(siteJS / Compile / dependencyFile).value,
    (siteJVM / Compile / resourceDirectory).value,
    siteTarget,
    None
  )
  // (siteJS / Compile / cssFile).value)

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

lazy val tests = Project("tests", binDir / "tests") settings (scala3Settings *) settings (assemblySettings *) settings(
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


lazy val modules = OsgiProject(binDir, "org.openmole.modules", singleton = true, imports = Seq("*")) settings(
  assemblySettings,
  scala3Settings,
  assemblyDependenciesPath := assemblyPath.value / "plugins",
  setExecutable ++= Seq("modules"),
  resourcesAssemble += {
    val bundle = OsgiKeys.bundle.value
    bundle -> (assemblyPath.value / "plugins" / bundle.getName)
  },
  //Compile / Osgi.bundleDependencies := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject, includeRoot = false))).value,
  resourcesAssemble ++= (Compile / Osgi.bundleDependencies).value.map(b => b → (assemblyPath.value / "plugins" / b.getName)),
  resourcesAssemble += ((Compile / resourceDirectory).value / "modules") -> (assemblyPath.value / "modules"),
  resourcesAssemble += (launcher / assemble).value -> (assemblyPath.value / "launcher"),
  libraryDependencies ++= requieredRuntimeLibraries,
  dependencyFilter := bundleFilter,
  dependencyName := rename,
  excludeTransitiveScala2,
  noNetLogoInClassPath
) dependsOn(openmoleBuildInfo, openmoleFile, module) dependsOn (toDependencies(openmoleDependencies) *)


lazy val launcher = OsgiProject(binDir, "org.openmole.launcher", imports = Seq("*"), settings = assemblySettings) settings(
  autoScalaLibrary := false,
  libraryDependencies += Libraries.equinoxOSGi,
  resourcesAssemble += {
    val bundle = (OsgiKeys.bundle).value
    bundle -> (assemblyPath.value / bundle.getName)
  },
  scala3Settings
)


lazy val consoleBin = OsgiProject(binDir, "org.openmole.console", imports = Seq("*")) settings(
  libraryDependencies += Libraries.jline,
  scala3Settings
) dependsOn(
  workflow,
  openmoleCompiler,
  openmoleProject,
  openmoleDSL,
  openmoleBuildInfo,
  module
)

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
  docker / buildOptions := BuildOptions(
    //cache = false,
    //removeIntermediateContainers = BuildOptions.Remove.Always,
    pullBaseImage = BuildOptions.Pull.Always
    //platforms = List("linux/arm64/v8"),
    //additionalArguments = Seq("--add-host", "127.0.0.1:12345", "--compress")
  ),
  docker / dockerfile := new Dockerfile {
    from("debian:testing")
    maintainer("Romain Reuillon <romain.reuillon@iscpif.fr>, Jonathan Passerat-Palmbach <j.passerat-palmbach@imperial.ac.uk>")
    runRaw(
      """echo "deb http://deb.debian.org/debian unstable main non-free contrib" >> /etc/apt/sources.list && \
       apt-get update && \
       apt-get install --no-install-recommends -y ca-certificates openjdk-21-jre-headless ca-certificates-java bash tar gzip sudo locales npm && \
       apt-get install -y singularity-container && \
       apt-get clean autoclean && apt-get autoremove --yes && rm -rf /var/lib/{apt,dpkg,cache,log}/ /var/lib/apt/lists/* && \
       mkdir -p /lib/modules && \
       sed -i '/^sessiondir max size/c\sessiondir max size = 0' /etc/singularity/singularity.conf""")
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
    copy((openmole / assemble).value, s"/openmole")
    runRaw(
      """chmod +x /openmole/openmole && \
        |ln -s /openmole/openmole /usr/bin/openmole""".stripMargin)
    copy((Compile / resourceDirectory).value / "openmole-docker", "/usr/bin/openmole-docker")
    runRaw("""chmod +x-w /usr/bin/openmole-docker""".stripMargin)
    volume("/var/openmole")
    expose(8443)
    cmdShell("openmole-docker")
  },
  generateDocker := {
    val dockerDir = target.value / "docker"
    val dockerFile = (docker / dockerfile).value.asInstanceOf[Dockerfile]
    val stagedDockerfile = sbtdocker.staging.DefaultDockerfileProcessor(dockerFile, dockerDir)
    IO.write(dockerDir / "Dockerfile", stagedDockerfile.instructionsString)
    stagedDockerfile.stageFiles.foreach {
      case (source, destination) => source.stage(destination)
    }
  }
)






