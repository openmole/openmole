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

package org.openmole.core.workspace

import java.io.File
import org.openmole.tool.file._
import com.thoughtworks.xstream.XStream

object Persistent {
  @transient lazy val xstream = new XStream
}

case class Persistent(baseDir: File) {

  baseDir.mkdirs

  def /(name: String) = Persistent(new File(baseDir, name))

  def save(obj: Any, name: String, xstream: XStream = Persistent.xstream) = {
    val file = new File(baseDir, name)
    file.withLock { _ ⇒ file.content = xstream.toXML(obj) }
  }

  def load[T](name: String, xstream: XStream = Persistent.xstream): T = {
    val file = new File(baseDir, name)
    loadFile(file, xstream)
  }

  protected def loadFile[T](file: File, xstream: XStream = Persistent.xstream): T = file.withLock { _ ⇒ xstream.fromXML(file.content).asInstanceOf[T] }

  def delete() = {
    baseDir.recursiveDelete
    baseDir.mkdirs
  }

  def all(xstream: XStream = Persistent.xstream) =
    baseDir.listRecursive(_.isFile).map { loadFile(_, xstream) }

}

