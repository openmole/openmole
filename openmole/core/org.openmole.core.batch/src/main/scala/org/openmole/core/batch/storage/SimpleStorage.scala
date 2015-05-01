/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.batch.storage

import fr.iscpif.gridscale
import fr.iscpif.gridscale.storage.{ Storage ⇒ GSStorage, FileType }
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.UUID
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.tool.file._
import org.openmole.core.tools.service.Logger

import scala.collection.JavaConversions._

trait SimpleStorage extends Storage {
  def exists(path: String): Boolean = _exists(path)
  def listNames(path: String): Seq[String] = _listNames(path)
  def list(path: String): Seq[(String, FileType)] = _list(path)
  def makeDir(path: String): Unit = _makeDir(path)
  def rmDir(path: String): Unit = _rmDir(path)
  def rmFile(path: String): Unit = _rmFile(path)
  def mv(from: String, to: String) = _mv(from, to)

  def openInputStream(path: String): InputStream = _openInputStream(path)
  def openOutputStream(path: String): OutputStream = _openOutputStream(path)

  def upload(src: File, dest: String, options: TransferOptions): Unit = _upload(src, dest, options)
  def download(src: String, dest: File, options: TransferOptions): Unit = _download(src, dest, options)

  def create(dest: String) = {
    val os = openOutputStream(dest)
    try os.append("")
    finally os.close
  }

}
