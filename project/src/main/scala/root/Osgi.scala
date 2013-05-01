package root

import aQute.lib.osgi.Builder
import java.util.Properties
import sbt._
import sbt.Keys._
import aQute.lib.osgi.Constants
import com.typesafe.sbt.osgi.OsgiManifestHeaders

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
    val fun = FileFunction.cached(target / "package-cache", FilesInfo.lastModified, FilesInfo.exists) {
      (changes: Set[File]) ⇒
        val builder = new Builder
        builder.setClasspath(fullClasspath map (_.data) toArray)
        builder.setProperties(headersToProperties(headers, additionalHeaders))
        //builder.setProperty(aQute.lib.osgi.Constants.INCLUDE_RESOURCE, "")
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

    fun(fullClasspath.map(_.data).toSet ++ resourceDirectories.toSet ++ embeddedJars.toSet).headOption getOrElse (target)
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

  private def defaultBundleSymbolicName(organization: String, name: String): String = {
    val organizationParts = parts(organization)
    val nameParts = parts(name)
    val partsWithoutOverlap = (organizationParts.lastOption, nameParts.headOption) match {
      case (Some(last), Some(head)) if (last == head) ⇒ organizationParts ++ nameParts.tail
      case _ ⇒ organizationParts ++ nameParts
    }
    partsWithoutOverlap mkString "."
  }

  private def id(s: String) = s

  private def parts(s: String) = s split "[.-]" filterNot (_.isEmpty)
}