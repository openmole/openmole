package org.openmole.buildsystem

import com.typesafe.sbt.osgi.{ OsgiKeys, SbtOsgi }

import sbt._
import Keys._
import util.matching.Regex

import com.typesafe.sbt.SbtScalariform.{ scalariformSettings, ScalariformKeys }

import scalariform.formatter.preferences._

import sbt.inc.Analysis
import resource._
import scala.tools.nsc.io.ZipArchive

trait BuildSystemDefaults extends Build {
  def dir: File

  def org: String

  def all: Project //An aggregate project that other parts of the build system use

  lazy val openMoleStandardVer = SettingKey[String]("openmoleversion")

  val eclipseBuddyPolicy = SettingKey[Option[String]]("OSGi.eclipseBuddyPolicy", "The eclipse buddy policy thing.")
  lazy val outDir = SettingKey[String]("outDir", "A setting to control where copyDepTask outputs it's dependencies")

  lazy val install = TaskKey[Unit]("install", "Builds bundles and adds them to the local repo")

  lazy val installRemote = TaskKey[Unit]("install-remote", "Builds bundles and adds them to the openmole nexus server")

  lazy val assemble = TaskKey[Unit]("assemble")

  lazy val gc = TaskKey[Unit]("gc", "Force SBT to take out the trash")

  lazy val osgiVersion = SettingKey[String]("osgi-version")

  lazy val osgiSingleton = SettingKey[Boolean]("osgi-singleton")

  lazy val Assemble = Tags.Tag("Assemble")

  lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")

  lazy val resourceOutDir = SettingKey[Option[String]]("resource-out-dir")

  lazy val resourceAssemble = TaskKey[Unit]("resource-assemble")

  lazy val ignoreTransitive = SettingKey[Boolean]("ignoreTransitive")

  lazy val dependencyFilter = SettingKey[DependencyFilter]("Tells copyDependencies to ignore certain dependencies.")

  lazy val dependencyNameMap = SettingKey[Map[Regex, String ⇒ String]]("dependencymap", "A map that is run against dependencies to be copied.")

  def copyResTask = resourceAssemble <<= (resourceDirectory, outDir, target, resourceOutDir) map { //TODO: Find a natural way to do this
    (rT, outD, cT, rOD) ⇒
      {
        val destPath = rOD map (cT / "assembly" / _) getOrElse (cT / "assembly" / outD)
        IO.copyDirectory(rT, destPath)
      }
  }

  /*def zipAssembly(target: File) = {
    val assembly = target / "assembly"

    for(tOS <- managed(new TarOutput))



  }*/

  override def settings = super.settings ++
    Seq(scalacOptions ++= Seq("-feature", "-language:reflectiveCalls", "-language:implicitConversions",
      "-language:existentials", "-language:postfixOps", "-Yinline-warnings"),
      osgiVersion := "3.8.2.v20130124-134944"
    )

  def gcTask = { System.gc(); System.gc(); System.gc() }

  def Aggregator(name: String) = Project(name, dir) settings (compile in Compile := Analysis.Empty)

  def copyDepTask(updateReport: UpdateReport, version: String, out: File,
    scalaVer: String, subDir: String,
    depMap: Map[Regex, String ⇒ String], depFilter: DependencyFilter) = {
    updateReport matching depFilter map { f ⇒
      depMap.keys.find(_.findFirstIn(f.getName).isDefined).map(depMap(_)).getOrElse { a: String ⇒ a } -> f
    } foreach {
      case (lambda, srcPath) ⇒
        val destPath = out / "assembly" / subDir / lambda(srcPath.getName)
        IO.copyFile(srcPath, destPath, preserveLastModified = true)
    }
  }

  protected lazy val scalariformDefaults = Seq(ScalariformKeys.preferences in Compile <<= ScalariformKeys.preferences(p ⇒
    p.setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(CompactControlReadability, true)
      .setPreference(PreserveDanglingCloseParenthesis, true))) ++ scalariformSettings

  protected lazy val osgiCachedSettings = Project.defaultSettings ++ SbtOsgi.osgiSettings ++ Seq(
    OsgiKeys.bundle <<= (
      OsgiKeys.manifestHeaders,
      OsgiKeys.additionalHeaders,
      fullClasspath in Compile,
      artifactPath in (Compile, packageBin),
      resourceDirectories in Compile,
      OsgiKeys.embeddedJars, target
    ) map Osgi.bundleTask,
    OsgiKeys.bundleSymbolicName <<= (name, osgiSingleton) { case (name, singleton) ⇒ name + ";singleton:=" + singleton },
    OsgiKeys.bundleVersion <<= version,
    OsgiKeys.exportPackage <<= (name) { n ⇒ Seq(n + ".*") },
    OsgiKeys.bundleActivator := None,
    install in Compile <<= publishLocal in Compile,
    installRemote in Compile <<= publish in Compile,
    OsgiKeys.bundle <<= OsgiKeys.bundle tag (Tags.Disk),
    (update in install) <<= update in install tag (Tags.Network),
    projectID <<= projectID { id =>
      id extra("osgified" -> "true")
    },
    publishTo <<= isSnapshot(if (_) Some("Openmole Nexus" at "http://maven.openmole.org/snapshots") else Some("Openmole Nexus" at "http://maven.openmole.org/releases")),
    credentials += Credentials(Path.userHome / ".sbt" / "openmole.credentials")
  ) ++ scalariformDefaults

  def OsgiSettings = osgiCachedSettings

  protected val bundleMap = Map("Bundle-ActivationPolicy" -> "lazy")

  def OsgiProject(artifactSuffix: String,
    pathFromDir: String = "",
    buddyPolicy: Option[String] = None,
    exports: Seq[String] = Seq(),
    privatePackages: Seq[String] = Seq(),
    singleton: Boolean = false,
    bundleActivator: Option[String] = None,
    dynamicImports: Seq[String] = Seq(),
    imports: Seq[String] = Seq("*;resolution:=optional"),
    embeddedJars: Seq[File] = Seq(), //TODO make this actually useful, using an EitherT or something
    openmoleScope: Option[String] = None)(implicit artifactPrefix: Option[String] = None) = {
    
    require(artifactPrefix.forall(!_.endsWith(".")), "Do not end your artifactprefix with ., it will be added automatically.")

    val artifactId = artifactPrefix map (_ + "." + artifactSuffix) getOrElse (artifactSuffix)
    val base = dir / (if (pathFromDir == "") artifactId else pathFromDir)
    val exportedPackages = if (exports.isEmpty) Seq(artifactId + ".*") else exports

    val additional = buddyPolicy.map(v ⇒ Map("Eclipse-BuddyPolicy" -> v)).getOrElse(Map()) ++
      openmoleScope.map(os ⇒ Map("OpenMOLE-Scope" -> os)).getOrElse(Map()) ++
      Map("Bundle-ActivationPolicy" -> "lazy")

    Project(artifactId.replace('.', '-'),
      base,
      settings = OsgiSettings ++
        Seq(name := artifactId, organization := org,
          osgiSingleton := singleton,
          OsgiKeys.exportPackage := exportedPackages,
          OsgiKeys.additionalHeaders := additional,
          OsgiKeys.privatePackage := privatePackages,
          OsgiKeys.dynamicImportPackage := dynamicImports,
          OsgiKeys.importPackage := imports,
          OsgiKeys.embeddedJars := embeddedJars,
          OsgiKeys.bundleActivator <<= (OsgiKeys.bundleActivator) { bA ⇒ bundleActivator.orElse(bA) })
    )
  }

  def AssemblyProject(base: String,
    outputDir: String = "lib",
    depNameMap: Map[Regex, String ⇒ String] = Map.empty[Regex, String ⇒ String]) = {
    val projBase = dir / base
    Project(base + "-" + outputDir.replace('/', '_'), projBase, settings = Project.defaultSettings ++ Seq(
      assemble <<= copyDependencies tag (Tags.Disk),
      install := true,
      installRemote := true,
      outDir := outputDir,
      resourceOutDir := None,
      dependencyNameMap := depNameMap,
      dependencyFilter := moduleFilter(),
      copyDependencies <<= (update, version, target, scalaVersion, outDir, dependencyNameMap, dependencyFilter) map copyDepTask
    ) ++ scalariformDefaults)
  }

  def provided(p: Project) = p % "provided"
}
