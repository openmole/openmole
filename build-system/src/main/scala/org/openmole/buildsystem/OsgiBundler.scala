package org.openmole.buildsystem

import sbt._
import Keys._
import OMKeys._
import com.typesafe.sbt.osgi.{ OsgiKeys, SbtOsgi }

trait OsgiBundler {
  self: BuildSystemDefaults ⇒

  protected val bundleMap = Map("Bundle-ActivationPolicy" → "lazy")

  protected def osgiSettings = SbtOsgi.autoImport.osgiSettings ++ Seq(
    OsgiKeys.bundleSymbolicName <<= (name, OSGi.singleton) { case (name, singleton) ⇒ name + ";singleton:=" + singleton },
    autoAPIMappings := true,
    bundleProj := true,
    OSGi.openMOLEScope := None,
    OsgiKeys.bundleVersion <<= version,
    OsgiKeys.exportPackage <<= name { n ⇒ Seq(n + ".*") },
    OsgiKeys.bundleActivator := None,
    OsgiKeys.requireCapability := """osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.7))""",
    install in Compile <<= publishLocal in Compile,
    installRemote in Compile <<= publish in Compile,
    OsgiKeys.bundle <<= OsgiKeys.bundle tag Tags.Disk,
    (update in install) <<= update in install tag Tags.Network,
    bundleType := Set("default"),
    test in (Test, test) <<= test in (Test, test) tag (Tags.Disk),
    publishTo <<= isSnapshot(if (_) Some("OpenMOLE Nexus" at "https://maven.openmole.org/snapshots") else Some("OpenMOLE Nexus" at "https://maven.openmole.org/releases"))
  ) ++ scalariformDefaults

  def OsgiProject(
    artifactSuffix:  String,
    pathFromDir:     String          = "",
    exports:         Seq[String]     = Seq(),
    privatePackages: Seq[String]     = Seq(),
    singleton:       Boolean         = false,
    settings:        Seq[Setting[_]] = Nil,
    bundleActivator: Option[String]  = None,
    dynamicImports:  Seq[String]     = Seq(),
    imports:         Seq[String]     = Seq("*;resolution:=optional"),
    global:          Boolean         = false
  )(implicit artifactPrefix: Option[String] = None) = {

    require(artifactPrefix.forall(!_.endsWith(".")), "Do not end your artifact prefix with ., it will be added automatically.")

    val artifactId = artifactPrefix map (_ + "." + artifactSuffix) getOrElse artifactSuffix
    val base = dir / (if (pathFromDir == "") artifactId else pathFromDir)
    val exportedPackages = if (exports.isEmpty) Seq(artifactId + ".*") else exports

    Project(artifactId.replace('.', '-'), base, settings = settings).enablePlugins(SbtOsgi).settings(commonsSettings ++ osgiSettings: _*).settings(
      name := artifactId,
      organization := org,
      OSGi.singleton := singleton,
      OsgiKeys.exportPackage := exportedPackages,
      OsgiKeys.additionalHeaders <<=
        (OSGi.openMOLEScope) {
          omScope ⇒
            Map[String, String]() +
              ("Bundle-ActivationPolicy" → "lazy") ++
              omScope.map(os ⇒ "OpenMOLE-Scope" → os) ++
              (if (global) Some("Eclipse-BuddyPolicy" → "global") else None)
        },
      OsgiKeys.privatePackage := privatePackages,
      OsgiKeys.dynamicImportPackage := dynamicImports,
      OsgiKeys.importPackage := imports,
      OsgiKeys.bundleActivator <<= OsgiKeys.bundleActivator { bA ⇒ bundleActivator.orElse(bA) }
    )
  }

  def OsgiGUIProject(
    name:   String,
    ext:    ClasspathDep[ProjectReference],
    client: ClasspathDep[ProjectReference],
    server: ClasspathDep[ProjectReference]
  ) = OsgiProject(name) dependsOn (ext, client, server)

}
