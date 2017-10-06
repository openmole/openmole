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

package org.openmole.plugin.environment.batch.storage

import java.io.{ ByteArrayInputStream, File, InputStream }

import gridscale.ListEntry
import org.openmole.core.communication.storage._
import org.openmole.core.workspace.NewFile

//trait SimpleStorage extends Storage {
//  def exists(path: String): Boolean = _exists(path)
//  def listNames(path: String): Seq[String] = _listNames(path)
//  def list(path: String): Seq[ListEntry] = _list(path)
//  def makeDir(path: String): Unit = _makeDir(path)
//  def rmDir(path: String): Unit = _rmDir(path)
//  def rmFile(path: String): Unit = _rmFile(path)
//  def mv(from: String, to: String) = _mv(from, to)
//  def uploadStream(src: InputStream, dest: String, options: TransferOptions = TransferOptions.default): Unit = _uploadStream(src, dest, options)
//  def downloadStream(src: String, options: TransferOptions = TransferOptions.default): InputStream = _downloadStream(src, options)
//  def upload(src: File, dest: String, options: TransferOptions = TransferOptions.default)(implicit newFile: NewFile): Unit = _upload(src, dest, options)
//  def download(src: String, dest: File, options: TransferOptions = TransferOptions.default)(implicit newFile: NewFile): Unit = _download(src, dest, options)
//
//}
