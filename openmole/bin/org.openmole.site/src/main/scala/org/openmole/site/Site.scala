/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.site

import java.io.File
import java.util.zip.GZIPInputStream
import ammonite.ops.Path
import com.ice.tar.TarInputStream
import org.eclipse.equinox.app._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._

import scala.sys.process.BasicIO

class Site extends IApplication {

  override def start(context: IApplicationContext) = {
    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]].map(_.trim)

    Config.testScript = !args.contains("-nc")

    val dest = new File(args(0))
    dest.recursiveDelete

    val site = new scalatex.site.Site {
      override def siteCss = Set.empty
      def content = Pages.all.map { p ⇒ p.file -> Pages.decorate(p) }.toMap
    }

    site.renderTo(Path(dest))

    for {
      r ← Resource.all
    } r match {
      case FileResource(name) ⇒
        val f = new File(dest, name)
        f.getParentFile.mkdirs
        f.withOutputStream { os ⇒
          withClosable(getClass.getClassLoader.getResourceAsStream(name)) { is ⇒
            BasicIO.transferFully(is, os)
          }
        }
      case ArchiveResource(name, dir) ⇒
        val f = new File(dest, dir)
        f.mkdirs
        withClosable(new TarInputStream(new GZIPInputStream(getClass.getClassLoader.getResourceAsStream(name)))) {
          _.extractDirArchiveWithRelativePath(f)
        }
    }

    withClosable(getClass.getClassLoader.getResourceAsStream(Resource.css)) { is ⇒
      withClosable(new File(dest, "styles.css").bufferedOutputStream(true)) { os ⇒
        BasicIO.transferFully(is, os)
      }
    }

    IApplication.EXIT_OK
  }

  override def stop() = {}

}
