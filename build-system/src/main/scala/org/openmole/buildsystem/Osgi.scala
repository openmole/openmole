package org.openmole.buildsystem

import aQute.lib.osgi.Builder
import java.util.Properties
import sbt._
import com.typesafe.sbt.osgi.OsgiManifestHeaders
import java.io.{ FileInputStream, FileOutputStream }
import resource._

object Osgi {

  def seqToStrOpt[A](seq: Seq[A])(f: A ⇒ String): Option[String] =
    if (seq.isEmpty) None else Some(seq map f mkString ",")

  def bundleTask(
    headers: OsgiManifestHeaders,
    additionalHeaders: Map[String, String],
    fullClasspath: Seq[Attributed[File]],
    artifactPath: File,
    resourceDirectories: Seq[File],
    embeddedJars: Seq[File], target: File): File = {
    val manifest = target / "manifest.xml"
    val props = headersToProperties(headers, additionalHeaders)
    val oldProps = new Properties()
    if (manifest.exists) managed(new FileInputStream(manifest)) foreach oldProps.load
    if (!oldProps.equals(props)) managed(new FileOutputStream(manifest)) foreach (props.store(_, ""))

    def expandClasspath(f: File): Array[File] = if (f.isDirectory) f.listFiles() flatMap expandClasspath else Array(f)

    val fun = FileFunction.cached(target / "package-cache", FilesInfo.lastModified, FilesInfo.exists) {
      (changes: Set[File]) ⇒
        val builder = new Builder
        builder.setClasspath(fullClasspath map (_.data) toArray)
        builder.setProperties(props)
        includeResourceProperty(resourceDirectories, embeddedJars) foreach (dirs ⇒
          builder.setProperty(aQute.lib.osgi.Constants.INCLUDE_RESOURCE, dirs)
        )
        bundleClasspathProperty(embeddedJars) foreach (jars ⇒
          builder.setProperty(aQute.lib.osgi.Constants.BUNDLE_CLASSPATH, jars)
        )
        val jar = builder.build
        jar.write(artifactPath)
        Set(artifactPath)
    }

    fun((fullClasspath flatMap (a ⇒ expandClasspath(a.data)) toSet) ++ resourceDirectories.toSet ++ embeddedJars.toSet + manifest).headOption getOrElse target
  }

  def headersToProperties(headers: OsgiManifestHeaders, additionalHeaders: Map[String, String]): Properties = {
    import headers._
    import aQute.lib.osgi.Constants._
    val properties = new Properties
    properties.put(BUNDLE_SYMBOLICNAME, bundleSymbolicName)
    properties.put(BUNDLE_VERSION, bundleVersion)
    bundleActivator foreach (properties.put(BUNDLE_ACTIVATOR, _))
    seqToStrOpt(dynamicImportPackage)(id) foreach (properties.put(DYNAMICIMPORT_PACKAGE, _))
    seqToStrOpt(exportPackage)(id) foreach (properties.put(EXPORT_PACKAGE, _))
    seqToStrOpt(importPackage)(id) foreach (properties.put(IMPORT_PACKAGE, _))
    fragmentHost foreach (properties.put(FRAGMENT_HOST, _))
    seqToStrOpt(privatePackage)(id) foreach (properties.put(PRIVATE_PACKAGE, _))
    seqToStrOpt(requireBundle)(id) foreach (properties.put(REQUIRE_BUNDLE, _))
    additionalHeaders foreach { case (k, v) ⇒ properties.put(k, v) }
    properties
  }

  private def includeResourceProperty(resourceDirectories: Seq[File], embeddedJars: Seq[File]) =
    seqToStrOpt(resourceDirectories ++ embeddedJars)(_.getAbsolutePath)

  private def bundleClasspathProperty(embeddedJars: Seq[File]) =
    seqToStrOpt(embeddedJars)(_.getName) map (".," + _)

  private def id(s: String) = s
}