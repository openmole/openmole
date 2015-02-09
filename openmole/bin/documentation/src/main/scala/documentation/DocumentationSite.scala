
/*
 * Copyright (C) 2014 Romain Reuillon
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
 */

package documentation

import java.util.zip.GZIPInputStream
import DocumentationPages._
import com.ice.tar.TarInputStream
import scala.sys.process.BasicIO
import scalatags.Text.all._
import ammonite.ops.Path
import java.io.File
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._

object DocumentationSite extends App {

  val dest = new File(args(0))
  dest.recursiveDelete

  for {
    r ← Resource.all
  } r match {
    case FileResource(name) ⇒
      val f = new File(dest, name)
      f.getParentFile.mkdirs
      f.withOutputStream { os ⇒
        val is = getClass.getClassLoader.getResourceAsStream(name)
        try BasicIO.transferFully(is, os)
        finally is.close
      }
    case ArchiveResource(name, dir) ⇒
      val f = new File(dest, dir)
      f.mkdirs
      val is = new TarInputStream(new GZIPInputStream(getClass.getClassLoader.getResourceAsStream(name)))
      try is.extractDirArchiveWithRelativePath(f)
      finally is.close
  }

  val site = new scalatex.site.Site {
    def content = Pages.all.map { p ⇒ p.file -> Pages.decorate(p) }.toMap
  }
  site.renderTo(Path(dest))

}
