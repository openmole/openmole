package src.main.scala.projectRoot

import com.typesafe.sbt.osgi.OsgiKeys
import com.typesafe.sbt.osgi.SbtOsgi
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

  lazy val install = TaskKey[Unit]("install", "Builds bundles and adds them to the local repo")
  lazy val assemble = TaskKey[Unit]("assemble")

  lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")

  def copyDepTask = copyDependencies <<= (update, version, crossTarget, scalaVersion) map {
    (updateReport, version, out, scalaVer) =>
      updateReport.allFiles filter {
        file => file.getName.contains(version)
      } foreach { srcPath =>
        val destPath = out / "lib" / srcPath.getName
        IO.copyFile(srcPath, destPath, preserveLastModified=true)
      }
    }


  override lazy val settings = super.settings ++
    Seq(version := "0.8.0-SNAPSHOT",
      organization := "org.openmole",
      scalaVersion := "2.10.1",
      resolvers ++= Seq("openmole" at "http://maven.openmole.org/snapshots",
        "openmole-releases" at "http://maven.openmole.org/public"),
      publishArtifact in (packageDoc in install) := false,
      copyDependencies := false
    )


  def OsgiProject(artifactId: String,
                  pathFromDir: String = "",
                   buddyPolicy: Option[String] = None,
                   exports: Seq[String] = Seq(),
                   privatePackages: Seq[String] = Seq())(implicit dir: File) = {

    val base = file(dir.getPath + "/" + (if(pathFromDir == "") artifactId else pathFromDir))
    val exportedPackages = if (exports.isEmpty) Seq(artifactId + ".*") else exports
    val additional = buddyPolicy.map(v => Map("Eclipse-BuddyPolicy" -> v)).getOrElse(Map()) ++ Map("Bundle-ActivationPolicy" -> "lazy")


    Project(artifactId.replace('.','-'),
      base,
      settings = Project.defaultSettings ++
        SbtOsgi.defaultOsgiSettings ++
        Seq(name := artifactId,
          OsgiKeys.bundleSymbolicName <<= name,
          OsgiKeys.bundleVersion <<= version,
          OsgiKeys.exportPackage := exportedPackages,
          OsgiKeys.additionalHeaders := additional,
          OsgiKeys.privatePackage := privatePackages,
          install <<= publishLocal,
          assemble <<= (install, copyDependencies) {(i,c) => i}))
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
        assemble := println("hello world"),
        assemble <<= assemble dependsOn (install, copyDependencies)
      )
  }

  def AssemblyProject(artifactId: String, location: File ) = {

  }
}
