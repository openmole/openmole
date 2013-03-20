package projectRoot

import com.typesafe.sbt.osgi.OsgiKeys
import com.typesafe.sbt.osgi.SbtOsgi
import scala.util.Random
import sbt._
import Keys._

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

  def copyDepTask = copyDependencies <<= (update, version, crossTarget, scalaVersion, outDir) map {
    (updateReport, version, out, scalaVer, outDir) =>
      println("copying dependencies")
      updateReport.allFiles filter {f =>
        println(f.getName)
        true
      } foreach { srcPath =>
        val destPath = out / outDir / srcPath.getName
        IO.copyFile(srcPath, destPath, preserveLastModified=true)
      }
    }


  override def settings = super.settings ++
    Seq(version := "0.8.0-SNAPSHOT",
      organization := "org.openmole",
      scalaVersion := "2.10.1",
      resolvers ++= Seq("openmole" at "http://maven.openmole.org/snapshots",
        "openmole-releases" at "http://maven.openmole.org/public"),
      externalResolvers <<= resolvers map {rs =>
        Resolver.withDefaultResolvers(Seq()) ++ rs
      },
      publishArtifact in (packageDoc in install) := false,
      copyDependencies := false,
      copyDependencies <<= copyDependencies tag (Assemble),
      outDir := "lib",
      concurrentRestrictions := Seq(Tags.exclusive(Assemble))
    )


  def OsgiProject(artifactId: String,
                  pathFromDir: String = "",
                   buddyPolicy: Option[String] = None,
                   exports: Seq[String] = Seq(),
                   privatePackages: Seq[String] = Seq())
                 (implicit dir: File,
                   org: Setting[String] = organization := "org.openmole") = {

    val base = dir / (if(pathFromDir == "") artifactId else pathFromDir)
    val exportedPackages = if (exports.isEmpty) Seq(artifactId + ".*") else exports
    val additional = buddyPolicy.map(v => Map("Eclipse-BuddyPolicy" -> v)).getOrElse(Map()) ++ Map("Bundle-ActivationPolicy" -> "lazy")


    Project(artifactId.replace('.','-'),
      base,
      settings = Project.defaultSettings ++
        SbtOsgi.osgiSettings ++
        Seq(name := artifactId, org,
          OsgiKeys.bundleSymbolicName <<= name,
          OsgiKeys.bundleVersion <<= version,
          OsgiKeys.exportPackage := exportedPackages,
          OsgiKeys.additionalHeaders := additional,
          OsgiKeys.privatePackage := privatePackages,
          install <<= publishLocal,
          assemble := false))
  }

  def OSGIProject(artifactId: String,
                  buddyPolicy: Option[String] = None,
                  exports: Seq[String] = Seq(),
                  privatePackages: Seq[String] = Seq()) = {
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
        install <<= publishLocal,
        assemble := println("hello world")
      )
  }

  def AssemblyProject(base: String, outputDir: String)(implicit dir: File) = {
    val projBase = dir / base
    Project(base, projBase, settings = Project.defaultSettings ++ Seq(copyDepTask,
      assemble <<= copyDependencies tag (Assemble), install := true))

  }
}
