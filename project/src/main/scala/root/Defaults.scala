package root

import com.typesafe.sbt.osgi.{ OsgiKeys, SbtOsgi }

import sbt._
import Keys._
import util.matching.Regex

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:43 PM
 * To change this template use File | Settings | File Templates.
 */
trait Defaults extends Build {
  def dir: File

  lazy val org = organization := "org.openmole"

  lazy val openMoleStandardVer = SettingKey[String]("openmoleversion")

  val eclipseBuddyPolicy = SettingKey[Option[String]]("OSGi.eclipseBuddyPolicy", "The eclipse buddy policy thing.")
  lazy val outDir = SettingKey[String]("outDir", "A setting to control where copyDepTask outputs it's dependencies")

  lazy val install = TaskKey[Unit]("install", "Builds bundles and adds them to the local repo")
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

  def copyResTask = resourceAssemble <<= (resourceDirectory, outDir, crossTarget, resourceOutDir) map { //TODO: Find a natural way to do this
    (rT, outD, cT, rOD) ⇒
      {
        val destPath = rOD map (cT / _) getOrElse (cT / outD)
        IO.copyDirectory(rT, destPath)
      }
  }

  override def settings = super.settings ++
    Seq(version := "0.9.0-SNAPSHOT",
      organization := "org.openmole",
      scalaVersion := "2.10.1",
      publishArtifact in (packageDoc in install) := false,
      publishArtifact in (packageSrc in install) := false,
      copyDependencies := false,
      //exportJars := true,
      osgiVersion := "3.8.2.v20130124-134944",
      //openMoleStandardVer := "0.8.0-RC3", //workaround for copy dep task issue
      concurrentRestrictions in Global :=
        Seq(
          Tags.limit(Tags.Disk, 2),
          Tags.limit(Tags.Network, 2),
          Tags.limitAll(8)
        )
    )

  def gcTask = { System.gc(); System.gc(); System.gc() }

  def copyDepTask(updateReport: UpdateReport, version: String, out: File,
                  scalaVer: String, subDir: String,
                  depMap: Map[Regex, String ⇒ String], depFilter: DependencyFilter) = { //TODO use this style for other tasks
    updateReport matching depFilter map { f ⇒
      depMap.keys.find(_.findFirstIn(f.getName).isDefined).map(depMap(_)).getOrElse { a: String ⇒ a } -> f
    } foreach {
      case (lambda, srcPath) ⇒
        val destPath = out / subDir / lambda(srcPath.getName)
        IO.copyFile(srcPath, destPath, preserveLastModified = true)
    }
  }

  def OsgiSettings = Project.defaultSettings ++ SbtOsgi.osgiSettings ++ Seq(
    OsgiKeys.bundleSymbolicName <<= (name, osgiSingleton) { case (name, singleton) ⇒ name + ";singleton:=" + singleton },
    OsgiKeys.bundleVersion <<= version,
    OsgiKeys.exportPackage <<= (name) { n ⇒ Seq(n + ".*") },
    OsgiKeys.bundleActivator := None,
    install in Compile <<= publishLocal in Compile,
    OsgiKeys.bundle <<= OsgiKeys.bundle tag (Tags.Disk),
    (update in install) <<= update in install tag (Tags.Network),
    assemble := OsgiKeys.bundle
  )

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
        Seq(name := artifactId, org,
          osgiSingleton := singleton,
          OsgiKeys.bundleSymbolicName <<= (name) { n ⇒ n + (if (singleton) ";singleton:=" + singleton else "") },
          OsgiKeys.bundleVersion <<= version,
          OsgiKeys.exportPackage := exportedPackages,
          OsgiKeys.additionalHeaders := additional,
          OsgiKeys.privatePackage := privatePackages,
          OsgiKeys.dynamicImportPackage := dynamicImports,
          OsgiKeys.importPackage := imports,
          OsgiKeys.embeddedJars := embeddedJars) ++
          Seq(OsgiKeys.bundleActivator <<= (OsgiKeys.bundleActivator) { bA ⇒ bundleActivator.orElse(bA) })
    )
  }

  def AssemblyProject(base: String,
                      outputDir: String = "lib",
                      depNameMap: Map[Regex, String ⇒ String] = Map.empty[Regex, String ⇒ String]) = {
    val projBase = dir / base
    Project(base + "-" + outputDir.replace('/', '_'), projBase, settings = Project.defaultSettings ++ Seq(
      assemble <<= copyDependencies tag (Tags.Disk),
      ignoreTransitive := true,
      install := true,
      outDir := outputDir,
      resourceOutDir := None,
      dependencyNameMap := depNameMap,
      dependencyFilter := moduleFilter(),
      copyDependencies <<= (update, version, crossTarget, scalaVersion, outDir, dependencyNameMap, dependencyFilter) map copyDepTask
    ))
  }
}
