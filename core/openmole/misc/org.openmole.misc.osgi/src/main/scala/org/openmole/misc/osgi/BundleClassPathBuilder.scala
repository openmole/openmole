/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.osgi

import java.io.{ InputStream, IOException, File }
import scala.tools.nsc.io.AbstractFile
import java.net.URL
import java.lang.String
import org.osgi.framework.{ ServiceReference, Bundle }
import collection.mutable.ListBuffer
import org.osgi.service.packageadmin.PackageAdmin

import collection.JavaConversions._

/**
 * Helper methods to transform OSGi bundles into {@link AbstractFile} implementations
 * suitable for use with the Scala compiler
 */
object BundleClassPathBuilder {

  object Strings {

    /**
     * Avoid using the Java 6 specific String.isEmpty method
     */
    def isEmpty(text: String) = text == null || text.length == 0
  }

  def allBundles = Activator.bundleContext.getBundles.filterNot(_.getBundleId == 0).map(create)

  /**
   *  Create a new  { @link AbstractFile } instance representing an
   * { @link org.osgi.framework.Bundle }
   *
   * @param bundle the bundle
   */
  def create(bundle: Bundle): AbstractFile = {
    //println("Create for " + bundle)

    require(bundle != null, "Bundle should not be null")

    abstract class BundleEntry(url: URL, parent: DirEntry) extends AbstractFile {
      require(url != null, "url must not be null")
      lazy val (path: String, name: String) = getPathAndName(url)
      lazy val fullName: String = (path :: name :: Nil).filter(n ⇒ !n.isEmpty).mkString("/")

      /**
       * @return null
       */
      def file: File = null

      /**
       * @return last modification time or 0 if not known
       */
      def lastModified: Long =
        try { url.openConnection.getLastModified }
        catch { case _ ⇒ 0 }

      @throws(classOf[IOException])
      def container: AbstractFile =
        valueOrElse(parent) {
          throw new IOException("No container")
        }

      @throws(classOf[IOException])
      def input: InputStream = url.openStream

      /**
       * Not supported. Always throws an IOException.
       * @throws IOException
       */
      @throws(classOf[IOException])
      def output = throw new IOException("not supported: output")

      private def getPathAndName(url: URL): (String, String) = {
        val u = url.getPath
        var k = u.length
        while ((k > 0) && (u(k - 1) == '/'))
          k = k - 1

        var j = k
        while ((j > 0) && (u(j - 1) != '/'))
          j = j - 1

        (u.substring(if (j > 0) 1 else 0, if (j > 1) j - 1 else j), u.substring(j, k))
      }

      override def toString = fullName
    }

    class DirEntry(url: URL, parent: DirEntry) extends BundleEntry(url, parent) {

      //println("Create dir entry " + url + " " + parent)
      /**
       * @return true
       */
      def isDirectory: Boolean = true

      override def iterator: Iterator[AbstractFile] = {
        new Iterator[AbstractFile]() {
          val dirs = bundle.getEntryPaths(fullName)
          var nextEntry = prefetch()

          def hasNext() = {
            if (nextEntry == null)
              nextEntry = prefetch()

            nextEntry != null
          }

          def next() = {
            if (hasNext()) {
              val entry = nextEntry
              nextEntry = null
              entry
            } else {
              throw new NoSuchElementException()
            }
          }

          private def prefetch() = {
            if (dirs.hasMoreElements) {
              val entry = dirs.nextElement.asInstanceOf[String]
              var entryUrl = bundle.getResource("/" + entry)

              // Bundle.getResource seems to be inconsistent with respect to requiring
              // a trailing slash
              if (entryUrl == null)
                entryUrl = bundle.getResource("/" + removeTralingSlash(entry))

              // If still null OSGi wont let use load that resource for some reason
              if (entryUrl == null) {
                null
              } else {
                if (entry.endsWith(".class"))
                  new FileEntry(entryUrl, DirEntry.this)
                else
                  new DirEntry(entryUrl, DirEntry.this)
              }
            } else
              null
          }

          private def removeTralingSlash(s: String): String =
            if (s == null || s.length == 0)
              s
            else if (s.last == '/')
              removeTralingSlash(s.substring(0, s.length - 1))
            else
              s
        }
      }

      def lookupName(name: String, directory: Boolean): AbstractFile = {
        val entry = bundle.getEntry(fullName + "/" + name)
        nullOrElse(entry) { entry ⇒
          if (directory)
            new DirEntry(entry, DirEntry.this)
          else
            new FileEntry(entry, DirEntry.this)
        }
      }

      override def lookupPathUnchecked(path: String, directory: Boolean) = lookupPath(path, directory)
      def lookupNameUnchecked(name: String, directory: Boolean) = lookupName(path, directory)

      def absolute = unsupported("absolute() is unsupported")
      def create = unsupported("create() is unsupported")
      def delete = unsupported("create() is unsupported")
    }

    class FileEntry(url: URL, parent: DirEntry) extends BundleEntry(url, parent) {

      //println("Create file entry " + url + " " + parent)

      /**
       * @return false
       */
      def isDirectory: Boolean = false
      override def sizeOption: Option[Int] = Some(bundle.getEntry(fullName).openConnection().getContentLength())
      def lookupName(name: String, directory: Boolean): AbstractFile = null

      override def lookupPathUnchecked(path: String, directory: Boolean) = lookupPath(path, directory)
      def lookupNameUnchecked(name: String, directory: Boolean) = lookupName(path, directory)

      def iterator = Iterator.empty

      def absolute = unsupported("absolute() is unsupported")
      def create = unsupported("create() is unsupported")
      def delete = unsupported("create() is unsupported")
    }

    new DirEntry(bundle.getResource("/"), null) {
      override def toString = "AbstractFile[" + bundle + "]"
    }
  }

  /**
   * Evaluate <code>f</code> on <code>s</code> if <code>s</code> is not null.
   * @param s
   * @param f
   * @return <code>f(s)</code> if s is not <code>null</code>, <code>null</code> otherwise.
   */
  def nullOrElse[S, T](s: S)(f: S ⇒ T): T =
    if (s == null) null.asInstanceOf[T]
    else f(s)

  /**
   * @param t
   * @param default
   * @return <code>t</code> or <code>default</code> if <code>null</code>.
   */
  def valueOrElse[T](t: T)(default: ⇒ T) =
    if (t == null) default
    else t

  //  /**
  //   * Create a list of AbstractFile instances, representing the bundle and its wired depedencies
  //   */
  //  def fromBundle(bundle: Bundle): List[AbstractFile] = {
  //    require(bundle != null, "Bundle should not be null")
  //
  //    // add the bundle itself
  //    val files = ListBuffer(create(bundle))
  //
  //    // also add all bundles that have exports wired to imports from this bundle
  //    files.appendAll(fromWires(bundle))
  //
  //    files.toList
  //  }
  //
  //  /**
  //   * Find bundles that have exports wired to the given and bundle
  //   */
  //  def fromWires(bundle: Bundle): List[AbstractFile] = {
  //    println("Checking OSGi bundle wiring for %s", bundle)
  //    val context = bundle.getBundleContext
  //    var ref: ServiceReference[_] = context.getServiceReference(classOf[PackageAdmin].getName)
  //
  //    if (ref == null) {
  //      println("PackageAdmin service is unavailable - unable to check bundle wiring information")
  //      return List()
  //    }
  //
  //    try {
  //      var admin: PackageAdmin = context.getService(ref).asInstanceOf[PackageAdmin]
  //      if (admin == null) {
  //        println("PackageAdmin service is unavailable - unable to check bundle wiring information")
  //        return List()
  //      }
  //      return fromWires(admin, bundle)
  //    } finally {
  //      context.ungetService(ref)
  //    }
  //  }
  //
  //  def fromWires(admin: PackageAdmin, bundle: Bundle): List[AbstractFile] = {
  //    val exported = admin.getExportedPackages(null: Bundle)
  //    val list = new ListBuffer[Bundle]
  //    for (pkg ← exported; if pkg.getExportingBundle.getBundleId != 0) {
  //      val bundles = pkg.getImportingBundles();
  //      if (bundles != null) {
  //        for (b ← bundles; if b.getBundleId == bundle.getBundleId) {
  //          println("Bundle imports %s from %s", pkg, pkg.getExportingBundle)
  //          if (b.getBundleId == 0) {
  //            println("Ignoring system bundle")
  //          } else {
  //            list += pkg.getExportingBundle
  //          }
  //        }
  //      }
  //    }
  //    list.map(create(_)).toList
  //  }

}

