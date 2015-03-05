package org.openmole.buildsystem

import sbt._
import scala.util.matching.Regex

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/5/13
 * Time: 3:43 PM
 */
object OMKeys {

  val bundleType = SettingKey[Set[String]]("bundle-type") //Default setting for osgiprojects is default.

  val bundleProj = SettingKey[Boolean]("bundle-proj")

  val openMoleStandardVer = SettingKey[String]("openmole-version")

  val eclipseBuddyPolicy = SettingKey[Option[String]]("OSGi.eclipseBuddyPolicy", "The eclipse buddy policy thing.")

  val assemblyPath = SettingKey[File]("assemblyPath", "A setting to control assembly outputs directory.")

  val assemblyDependenciesPath = SettingKey[File]("assemblyDependenciesPath", "A setting to control assembly outputs directory for dependencies.")

  val install = TaskKey[Unit]("install", "Builds bundles and adds them to the local repo")

  val installRemote = TaskKey[Unit]("install-remote", "Builds bundles and adds them to the openmole nexus server")

  val assemble = TaskKey[File]("assemble", "The path with assembled project")

  val setExecutable = SettingKey[Seq[String]]("setExecutable", "Sets the path relative to the assemble folder executable")

  val downloads = SettingKey[Seq[(URL, String)]]("downloads", "A project setting that describes a urls to download")

  val resourcesAssemble = TaskKey[Seq[(File, File)]]("resourcesAssemble", "A set of (in,out) tuples that specify where to find the resource (in) and what sub-path of assembly to put it in (out)")

  val ignoreTransitive = SettingKey[Boolean]("ignoreTransitive")

  val dependencyFilter = SettingKey[ModuleID ⇒ Boolean]("dependency-filter", "Tells copyDependencies to ignore certain dependencies.")

  val dependencyNameMap = SettingKey[Map[Regex, String ⇒ String]]("dependency-map", "A map that is run against dependencies to be copied.")

  val scalatestVersion = SettingKey[String]("scalatest-version", "Version of scalatest.")

  val junitVersion = SettingKey[String]("junit-version", "Version of junit.")

  object Tar {
    val tar = TaskKey[File]("tar", "Tar file produced by the assembly project")
    val innerFolder = SettingKey[String]("tar-inner-folder", "All files in tar will be put under this folder")
    val name = SettingKey[String]("tar-name")
    val folder = TaskKey[File]("tar-folder", "The folder to tar.")
  }

  object OSGiApplication {
    val pluginsDirectory = SettingKey[File]("osgi-plugins-directory")
    val config = SettingKey[File]("osgi-config")
    val startLevels = SettingKey[Seq[(String, Int)]]("osgi-start-levels")
    val header = SettingKey[String]("osgi-header")
  }

  object OSGi {
    val singleton = SettingKey[Boolean]("osgi-singleton")
    val openMOLEScope = SettingKey[Option[String]]("openmole-scope")
  }

}
