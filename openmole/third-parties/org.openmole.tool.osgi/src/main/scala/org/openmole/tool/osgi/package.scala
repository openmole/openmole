/**
 * Created by Romain Reuillon on 05/05/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
 *
 */
package org.openmole.tool

import java.io.{ ByteArrayInputStream, File }
import java.util.jar.*
import java.util.zip.ZipEntry

import org.openmole.tool.file.*
import org.openmole.tool.stream.*
import org.openmole.tool.bytecode.*
import org.osgi.framework.{ Bundle, Constants }

package object osgi:

  case class VersionedPackage(name: String, version: Option[String])

  def createBundle(
    name:             String,
    version:          String,
    classes:          Seq[ClassSource],
    exportedPackages: Seq[String],
    importedPackages: Seq[VersionedPackage],
    bundle:           File
  ) = {
    def versionedToString(p: VersionedPackage) = s"${p.name}${p.version.map(v => s""";${Constants.VERSION_ATTRIBUTE}="$v"""").getOrElse("")}"

    val manifest =
      s"""${Attributes.Name.MANIFEST_VERSION}: 1.0
        |${Constants.BUNDLE_SYMBOLICNAME}: $name
        |${Constants.BUNDLE_NAME}: $name
        |${Constants.BUNDLE_VERSION}: $version
        |${Constants.EXPORT_PACKAGE}: ${exportedPackages.mkString(",")}
        |${Constants.IMPORT_PACKAGE}: ${importedPackages.map(p => versionedToString(p)).mkString(",")}""".stripMargin

    val os = new JarOutputStream(bundle.bufferedOutputStream())
    try {
      os.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"))
      copy(new StringInputStream(manifest), os)
      os.closeEntry()
      for {
        cbc ← classes
      } {
        os.putNextEntry(new ZipEntry(ClassSource.path(cbc)))
        val is = ClassSource.openInputStream(cbc)
        try copy(is, os) finally is.close()
        os.closeEntry()
      }
    }
    finally os.close
  }

