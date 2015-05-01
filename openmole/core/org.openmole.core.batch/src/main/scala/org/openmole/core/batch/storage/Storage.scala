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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.core.batch.storage

import java.io.{ File, OutputStream, InputStream }
import java.util.UUID

import fr.iscpif.gridscale.storage._
import org.openmole.core.workspace._
import org.openmole.tool.thread._
import org.openmole.tool.file._

object TransferOptions {
  implicit def default = TransferOptions()
}

case class TransferOptions(raw: Boolean = false)

object Storage {
  val BufferSize = new ConfigurationLocation("Storage", "BufferSize")
  val CopyTimeout = new ConfigurationLocation("Storage", "CopyTimeout")
  val CloseTimeout = new ConfigurationLocation("Storage", "CloseTimeout")

  Workspace += (BufferSize, "65535")
  Workspace += (CopyTimeout, "PT1M")
  Workspace += (CloseTimeout, "PT1M")

  def uniqName(prefix: String, sufix: String) = prefix + "_" + UUID.randomUUID.toString + sufix
}

trait Storage {
  def root: String
  def child(parent: String, child: String): String
  protected def _exists(path: String): Boolean
  protected def _listNames(path: String): Seq[String]
  protected def _list(path: String): Seq[(String, FileType)]
  protected def _makeDir(path: String): Unit
  protected def _rmDir(path: String): Unit
  protected def _rmFile(path: String): Unit
  protected def _openInputStream(path: String): InputStream
  protected def _openOutputStream(path: String): OutputStream
  protected def _mv(from: String, to: String)

  protected def _upload(src: File, dest: String, options: TransferOptions) = {
    val os = if (!options.raw) _openOutputStream(dest).toGZ else _openOutputStream(dest)
    try src.copy(os, bufferSize, copyTimeout)
    finally timeout(os.close)(closeTimeout)
  }

  protected def _download(src: String, dest: File, options: TransferOptions) = {
    val is = if (!options.raw) _openInputStream(src).toGZ else _openInputStream(src)
    try is.copy(dest, bufferSize, copyTimeout)
    finally timeout(is.close)(closeTimeout)
  }

  private def bufferSize = Workspace.preferenceAsInt(Storage.BufferSize)
  private def copyTimeout = Workspace.preferenceAsDuration(Storage.CopyTimeout)
  private def closeTimeout = Workspace.preferenceAsDuration(Storage.CloseTimeout)
}
