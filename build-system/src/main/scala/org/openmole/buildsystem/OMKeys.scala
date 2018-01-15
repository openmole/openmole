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

  val openMoleStandardVer = SettingKey[String]("openmole-version")

  val assemblyPath = SettingKey[File]("assemblyPath", "A setting to control assembly outputs directory.")

  val assemblyDependenciesPath = SettingKey[File]("assemblyDependenciesPath", "A setting to control assembly outputs directory for dependencies.")

  val install = TaskKey[Unit]("install", "Builds bundles and adds them to the local repo")

  val installRemote = TaskKey[Unit]("install-remote", "Builds bundles and adds them to the openmole nexus server")

  val assemble = TaskKey[File]("assemble", "The path with assembled project")

  val setExecutable = SettingKey[Seq[String]]("setExecutable", "Sets the path relative to the assemble folder executable")

  val downloads = SettingKey[Seq[(URL, String)]]("downloads", "A project setting that describes a urls to download")

  val resourcesAssemble = TaskKey[Seq[(File, File)]]("resourcesAssemble", "A set of (in,out) tuples that specify where to find the resource (in) and what sub-path of assembly to put it in (out)")

  val ignoreTransitive = SettingKey[Boolean]("ignoreTransitive")

  val dependencyFilter = SettingKey[(ModuleID, Artifact) ⇒ Boolean]("dependency-filter", "Tells copyDependencies to ignore certain dependencies.")

  val dependencyName = SettingKey[ModuleID ⇒ String]("dependency-map", "A map that is run against dependencies to be copied.")

  val scalatestVersion = SettingKey[String]("scalatest-version", "Version of scalatest.")

  val junitVersion = SettingKey[String]("junit-version", "Version of junit.")

  object Osgi {
    val singleton = SettingKey[Boolean]("osgi-singleton")
    val openMOLEScope = SettingKey[Seq[String]]("openmole-scope")
    val bundleDependencies = TaskKey[Seq[File]]("bundle-dependencies")
  }

}
