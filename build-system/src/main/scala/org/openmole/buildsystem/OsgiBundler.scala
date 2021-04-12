package org.openmole.buildsystem

import sbt._
import Keys._
import OMKeys._
import com.typesafe.sbt.osgi.{ OsgiKeys, SbtOsgi }

object OsgiProject {

  protected val bundleMap = Map("Bundle-ActivationPolicy" → "lazy")

  protected def osgiSettings = SbtOsgi.autoImport.osgiSettings ++ Seq(
    OsgiKeys.bundleSymbolicName := (name.value + ";singleton:=" + Osgi.singleton.value),
    autoAPIMappings := true,

    Compile / Osgi.bundleDependencies := OsgiKeys.bundle.all(ScopeFilter(inDependencies(ThisProject))).value,

    Osgi.openMOLEScope := Seq.empty,
    OsgiKeys.bundleVersion := version.value,
    OsgiKeys.exportPackage := (name { n ⇒ Seq(n + ".*") }).value,
    OsgiKeys.bundleActivator := None,

    Compile / install := (Compile / publishLocal).value,
    Compile / installRemote := (Compile / publish).value,
    bundleType := Set("default"))

  def apply(
    directory: File,
    artifactId: String,
    exports: Seq[String] = Seq(),
    privatePackages: Seq[String] = Seq(),
    singleton: Boolean = false,
    settings: Seq[Setting[_]] = Nil,
    bundleActivator: Option[String] = None,
    dynamicImports: Seq[String] = Seq(),
    imports: Seq[String] = Seq("*;resolution:=optional"),
    global: Boolean = false) = {

    val base = directory / artifactId
    val exportedPackages = if (exports.isEmpty) Seq(artifactId + ".*") else exports

    Project(artifactId.replace('.', '-'), base).settings(settings: _*).enablePlugins(SbtOsgi).settings(osgiSettings: _*).settings(
      name := artifactId,
      Osgi.singleton := singleton,
      OsgiKeys.exportPackage := exportedPackages,
      OsgiKeys.additionalHeaders :=
        ((Osgi.openMOLEScope) {
          omScope ⇒
            Map[String, String]() +
              ("Bundle-ActivationPolicy" → "lazy") ++
              (if (!omScope.isEmpty) Some("OpenMOLE-Scope" → omScope.mkString(",")) else None) ++
              (if (global) Some("Eclipse-BuddyPolicy" → "global") else None)
        }).value,
      OsgiKeys.requireCapability := """osgi.ee; osgi.ee="JavaSE";version:List="1.8,1.9""""",
      //OsgiKeys.bundleRequiredExecutionEnvironment := Seq("JavaSE-1.8", "JavaSE-1.9"),
      OsgiKeys.privatePackage := privatePackages,
      OsgiKeys.dynamicImportPackage := dynamicImports,
      OsgiKeys.importPackage := imports,
      OsgiKeys.bundleActivator := (OsgiKeys.bundleActivator { bA ⇒ bundleActivator.orElse(bA) }).value)
  }
}

object OsgiGUIProject {

  def apply(
    directory: File,
    artifactId: String,
    ext: ClasspathDep[ProjectReference],
    client: ClasspathDep[ProjectReference],
    server: ClasspathDep[ProjectReference]) = OsgiProject(directory, artifactId) dependsOn (ext, client, server)

}
