package projectRoot

import com.typesafe.sbt.osgi.{OsgiKeys, SbtOsgi}

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
  val eclipseBuddyPolicy = SettingKey[Option[String]]("OSGi.eclipseBuddyPolicy", "The eclipse buddy policy thing.")
  lazy val outDir = SettingKey[String]("outDir", "A setting to control where copyDepTask outputs it's dependencies")

  lazy val install = TaskKey[Unit]("install", "Builds bundles and adds them to the local repo")
  lazy val assemble = TaskKey[Unit]("assemble")

  lazy val Assemble = Tags.Tag("Assemble")

  lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")

  lazy val resourceAssemble = TaskKey[Unit]("resource-assemble")

  lazy val ignoreTransitive = SettingKey[Boolean]("ignoreTransitive")

  lazy val dependencyFilter = SettingKey[Regex]("Tells copyDependencies to ignore certain ones.")

  lazy val dependencyNameMap = SettingKey[Map[Regex, String => String]]("dependencymap", "A map that is run against dependencies to be copied.")

  def copyResTask = resourceAssemble <<= (resourceDirectory, outDir, crossTarget) map { //TODO: Find a natural way to do this
    (rT, outD, cT) => {
      val destPath = cT / outD
      IO.copyDirectory(rT,destPath)
    }
  }

  def copyDepTask(updateReport: UpdateReport, version: String, out: File, scalaVer: String, subDir: String, depMap: Map[Regex, String => String]) = { //TODO use this style for other tasks
    updateReport.allFiles map {f =>
      depMap.keys.find(_.findFirstIn(f.getName).isDefined).map(depMap(_)).getOrElse{a: String => a} -> f
    } foreach { case(lambda, srcPath) =>
      val destPath = out / subDir / lambda(srcPath.getName)
      IO.copyFile(srcPath, destPath, preserveLastModified=true)
    }
  }


  override def settings = super.settings ++
    Seq(version := "0.8.0-SNAPSHOT",
      organization := "org.openmole",
      scalaVersion := "2.10.1",
      resolvers ++= Seq("openmole" at "http://maven.openmole.org/snapshots",
        "openmole-releases" at "http://maven.openmole.org/public",
        "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"),
      externalResolvers <<= (resolvers) map {rs =>
        Resolver.withDefaultResolvers(Seq()) ++ rs
      },
      publishArtifact in (packageDoc in install) := false,
      copyDependencies := false,
      concurrentRestrictions := Seq(Tags.exclusive(Assemble))
    )


  def OsgiProject(artifactId: String,
                  pathFromDir: String = "",
                   buddyPolicy: Option[String] = None,
                   exports: Seq[String] = Seq(),
                   privatePackages: Seq[String] = Seq(),
                   singleton: Boolean = false,
                   dynamicImports: Seq[String] = Seq(),
                   imports: Seq[String] = Seq("*;resolution:=optional"))
                 (implicit dir: File,
                   org: Setting[String] = organization := "org.openmole") = {

    val base = dir / (if(pathFromDir == "") artifactId else pathFromDir)
    val exportedPackages = if (exports.isEmpty) Seq(artifactId + ".*") else exports
    val additional = buddyPolicy.map(v => Map("Eclipse-BuddyPolicy" -> v)).getOrElse(Map()) ++
      Map("Bundle-ActivationPolicy" -> "lazy")


    Project(artifactId.replace('.','-'),
      base,
      settings = Project.defaultSettings ++
        SbtOsgi.osgiSettings ++
        Seq(name := artifactId, org,
          OsgiKeys.bundleSymbolicName <<= (name) {n => n + (if(singleton) ";singleton:=" + singleton else "")},
          OsgiKeys.bundleVersion <<= version,
          OsgiKeys.exportPackage := exportedPackages,
          OsgiKeys.additionalHeaders := additional,
          OsgiKeys.privatePackage := privatePackages,
          OsgiKeys.dynamicImportPackage := dynamicImports,
          OsgiKeys.importPackage := imports,
          install <<= publishLocal,
          assemble := false))
  }

  def OSGIProject(artifactId: String,
                  buddyPolicy: Option[String] = None,
                  exports: Seq[String] = Seq(),
                  privatePackages: Seq[String] = Seq(),
                  dynamicImports: Seq[String] = Seq()) = {
    val exportedPackages = if (exports.isEmpty) Seq(artifactId + ".*") else exports
    val additional = buddyPolicy.map(v => Map("Eclipse-BuddyPolicy" -> v)).getOrElse(Map()) ++ Map("Bundle-ActivationPolicy" -> "lazy")
    Project.defaultSettings ++
      SbtOsgi.osgiSettings ++
      Seq(name:= artifactId,
        OsgiKeys.bundleSymbolicName <<= name,
        OsgiKeys.bundleVersion <<= version,
        OsgiKeys.exportPackage := exportedPackages,
        OsgiKeys.additionalHeaders := additional,
        OsgiKeys.privatePackage := privatePackages,
        OsgiKeys.dynamicImportPackage := dynamicImports,
        install <<= publishLocal,
        assemble := false
      )
  }

  def AssemblyProject(base: String,
                      outputDir: String = "lib",
                      depNameMap: Map[Regex, String => String] = Map.empty[Regex, String => String])
                     (implicit dir: File) = {
    val projBase = dir / base
    Project(base + "-"+ outputDir, projBase, settings = Project.defaultSettings ++ Seq(
      assemble <<= copyDependencies tag (Assemble),
      install := true,
      outDir := outputDir,
      dependencyNameMap := depNameMap,
      copyDependencies <<= (update, version, crossTarget, scalaVersion, outDir, dependencyNameMap) map copyDepTask
    ))
  }
}
