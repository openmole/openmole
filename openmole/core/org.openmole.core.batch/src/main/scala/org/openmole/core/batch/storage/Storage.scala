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
import java.nio.file.Files
import java.util.UUID

import fr.iscpif.gridscale.storage._
import org.openmole.core.workspace._
import org.openmole.tool.thread._
import org.openmole.tool.file._

object TransferOptions {
  implicit def default = TransferOptions()
}

case class TransferOptions(raw: Boolean = false, forceCopy: Boolean = false, canMove: Boolean = false)

object Storage {
  val BufferSize = new ConfigurationLocation("Storage", "BufferSize")
  val CopyTimeout = new ConfigurationLocation("Storage", "CopyTimeout")
  val CloseTimeout = new ConfigurationLocation("Storage", "CloseTimeout")
  Workspace += (BufferSize, "65535")
  Workspace += (CopyTimeout, "PT1M")
  Workspace += (CloseTimeout, "PT1M")

  def uniqName(prefix: String, sufix: String) = prefix + "_" + UUID.randomUUID.toString + sufix

}

trait CompressedTransfer <: Storage {

  override abstract def _uploadStream(src: InputStream, dest: String, options: TransferOptions) =
    if (!options.raw) super._uploadStream(src.toGZiped, dest, options) else super._uploadStream(src, dest, options)

  override abstract protected def _downloadStream(src: String, options: TransferOptions) =
    if (!options.raw) super._downloadStream(src, options).toGZ else super._downloadStream(src, options)

}

trait Storage {
  def root: String
  def child(parent: String, child: String): String
  protected def _exists(path: String): Boolean
  protected def _listNames(path: String): Seq[String]
  protected def _list(path: String): Seq[ListEntry]
  protected def _makeDir(path: String): Unit
  protected def _rmDir(path: String): Unit
  protected def _rmFile(path: String): Unit
  protected def _mv(from: String, to: String)
  protected def _parent(path: String): Option[String]
  protected def _name(path: String): String
  protected def _uploadStream(src: InputStream, dest: String, options: TransferOptions): Unit
  protected def _downloadStream(src: String, options: TransferOptions): InputStream

  protected def _upload(src: File, dest: String, options: TransferOptions): Unit =
    src.withInputStream(is ⇒ _uploadStream(is, dest, options))

  protected def _download(src: String, dest: File, options: TransferOptions): Unit = {
    val is = _downloadStream(src, options)
    try Files.copy(is, dest.toPath)
    finally is.close()
  }

  protected def bufferSize = Workspace.preferenceAsInt(Storage.BufferSize)
  protected def copyTimeout = Workspace.preferenceAsDuration(Storage.CopyTimeout)
  protected def closeTimeout = Workspace.preferenceAsDuration(Storage.CloseTimeout)
}
