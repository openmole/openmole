package org.openmole.buildsystem

/*import aQute.bnd.osgi.Builder
import aQute.bnd.osgi.Constants._
import java.util.Properties
import sbt._
import com.typesafe.sbt.osgi.OsgiManifestHeaders
import java.io.{ FileInputStream, FileOutputStream }
import resource._

private object Osgi {
  def bundleTask(
    headers: OsgiManifestHeaders,
    resources: Seq[String],
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

    def expand(f: File): Array[File] = if (f.isDirectory) f.listFiles() flatMap expand else Array(f)

    val fun = FileFunction.cached(target / "package-cache", FilesInfo.lastModified, FilesInfo.exists) {
      (changes: Set[File]) ⇒
        val builder = new Builder
        builder.setClasspath(fullClasspath map (_.data) toArray)
        builder.setProperties(props)
        includeResourceProperty(resourceDirectories, embeddedJars, resources) foreach (dirs ⇒
          builder.setProperty(INCLUDERESOURCE, dirs)
        )
        bundleClasspathProperty(embeddedJars) foreach (jars ⇒
          builder.setProperty(BUNDLE_CLASSPATH, jars)
        )
        val jar = builder.build
        println(jar.getResources)
        jar.write(artifactPath)
        Set(artifactPath)
    }

    fun((fullClasspath flatMap (a ⇒ expand(a.data)) toSet) ++ (resourceDirectories flatMap expand).toSet ++ embeddedJars.toSet + manifest).headOption getOrElse target
  }

  def headersToProperties(headers: OsgiManifestHeaders, additionalHeaders: Map[String, String]): Properties = {
    import headers._
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

  def seqToStrOpt[A](seq: Seq[A])(f: A ⇒ String): Option[String] =
    if (seq.isEmpty) None else Some(seq map f mkString ",")

  def includeResourceProperty(resourceDirectories: Seq[File], embeddedJars: Seq[File], oResources: Seq[String]): Option[String] = {
    seqToStrOpt(resourceDirectories ++ embeddedJars)(_.getAbsolutePath) zip seqToStrOpt(oResources)(a ⇒ a) map { case (a, b) ⇒ a + (if (!b.isEmpty) "," else "") + b } headOption
  }

  def bundleClasspathProperty(embeddedJars: Seq[File]) =
    seqToStrOpt(embeddedJars)(_.getName) map (".," + _)

  def defaultBundleSymbolicName(organization: String, name: String): String = {
    val organizationParts = parts(organization)
    val nameParts = parts(name)
    val partsWithoutOverlap = (organizationParts.lastOption, nameParts.headOption) match {
      case (Some(last), Some(head)) if (last == head) ⇒ organizationParts ++ nameParts.tail
      case _ ⇒ organizationParts ++ nameParts
    }
    partsWithoutOverlap mkString "."
  }

  def id(s: String) = s

  def parts(s: String) = s split "[.-]" filterNot (_.isEmpty)
}*/ 