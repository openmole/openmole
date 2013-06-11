package org.openmole.buildsystem

import sbt._
import Keys._
import OMKeys._
import com.typesafe.sbt.osgi.{ OsgiKeys, SbtOsgi }

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/5/13
 * Time: 3:57 PM
 */
trait OsgiBundler { self: BuildSystemDefaults ⇒

  protected val bundleMap = Map("Bundle-ActivationPolicy" -> "lazy")

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
    OsgiKeys.exportPackage <<= name { n ⇒ Seq(n + ".*") },
    OsgiKeys.bundleActivator := None,
    install in Compile <<= publishLocal in Compile,
    installRemote in Compile <<= publish in Compile,
    OsgiKeys.bundle <<= OsgiKeys.bundle tag Tags.Disk,
    (update in install) <<= update in install tag Tags.Network,
    bundleType := "default",
    projectID <<= (projectID, bundleType) { (id, bT) ⇒
      id extra ("project-name" -> projectName, "bundle-type" -> bT)
    },
    publishTo <<= isSnapshot(if (_) Some("Openmole Nexus" at "http://maven.openmole.org/snapshots") else Some("Openmole Nexus" at "http://maven.openmole.org/releases")),
    credentials += Credentials(Path.userHome / ".sbt" / "openmole.credentials")
  ) ++ scalariformDefaults

  def OsgiSettings = osgiCachedSettings

  def OsgiProject(artifactSuffix: String,
                  pathFromDir: String = "",
                  buddyPolicy: Option[String] = None,
                  exports: Seq[String] = Seq(),
                  privatePackages: Seq[String] = Seq(),
                  singleton: Boolean = false,
                  settings: Seq[Project.Setting[_]] = Nil,
                  bundleActivator: Option[String] = None,
                  dynamicImports: Seq[String] = Seq(),
                  imports: Seq[String] = Seq("*;resolution:=optional"),
                  embeddedJars: Seq[File] = Seq(), //TODO make this actually useful, using an EitherT or something
                  openmoleScope: Option[String] = None)(implicit artifactPrefix: Option[String] = None) = {

    require(artifactPrefix.forall(!_.endsWith(".")), "Do not end your artifactprefix with ., it will be added automatically.")

    val artifactId = artifactPrefix map (_ + "." + artifactSuffix) getOrElse artifactSuffix
    val base = dir / (if (pathFromDir == "") artifactId else pathFromDir)
    val exportedPackages = if (exports.isEmpty) Seq(artifactId + ".*") else exports
    val sets = settings

    val additional = buddyPolicy.map(v ⇒ Map("Eclipse-BuddyPolicy" -> v)).getOrElse(Map()) ++
      openmoleScope.map(os ⇒ Map("OpenMOLE-Scope" -> os)).getOrElse(Map()) ++
      Map("Bundle-ActivationPolicy" -> "lazy")

    Project(artifactId.replace('.', '-'),
      base,
      settings = OsgiSettings ++ Seq(
        name := artifactId, organization := org,
        osgiSingleton := singleton,
        OsgiKeys.exportPackage := exportedPackages,
        OsgiKeys.additionalHeaders := additional,
        OsgiKeys.privatePackage := privatePackages,
        OsgiKeys.dynamicImportPackage := dynamicImports,
        OsgiKeys.importPackage := imports,
        OsgiKeys.embeddedJars := embeddedJars,
        OsgiKeys.bundleActivator <<= OsgiKeys.bundleActivator { bA ⇒ bundleActivator.orElse(bA) }
      ) ++ sets)
  }

}
