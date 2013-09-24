/*
 * Copyright (C) 23/09/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.workspace

import java.io.File
import org.openmole.misc.tools.io.FileUtil._
import com.thoughtworks.xstream.XStream

object Persistent {
  @transient lazy val xstream = new XStream
}

import Persistent._

case class Persistent(baseDir: File) {

  baseDir.mkdirs

  private def subDirectory(dir: Option[String]) =
    dir.map(new File(baseDir, _)).getOrElse(baseDir)

  def save(obj: Any, name: String, category: Option[String] = None) = synchronized {
    val subDir = subDirectory(category)
    subDir.mkdirs()
    val file = new File(subDir, name)
    file.content = xstream.toXML(obj)
  }

  def load(name: String, category: Option[String] = None) = synchronized {
    val subDir = subDirectory(category)
    val file = new File(subDir, name)
    xstream.fromXML(file.content)
  }

  def clean(category: Option[String] = None) = synchronized {
    subDirectory(category).recursiveDelete
  }

  def all(category: Option[String] = None) = synchronized {
    subDirectory(category).listRecursive(_.isFile).map {
      f ⇒ xstream.fromXML(f.content)
    }
  }

}

