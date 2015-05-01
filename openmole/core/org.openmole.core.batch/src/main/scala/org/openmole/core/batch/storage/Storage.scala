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

package org.openmole.core.batch.storage

import fr.iscpif.gridscale.storage.{ Storage ⇒ GSStorage, FileType }
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import org.openmole.tool.file._
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import org.openmole.tool.thread._

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

  val storage: GSStorage

  def root: String

  def child(parent: String, child: String): String = storage.child(parent, child)

  protected def exists(path: String): Boolean = storage.exists(path)
  protected def listNames(path: String): Seq[String] = storage.listNames(path)
  protected def list(path: String): Seq[(String, FileType)] = storage.list(path)
  protected def makeDir(path: String): Unit = storage.makeDir(path)
  protected def rmDir(path: String): Unit = storage.rmDir(path)
  protected def rmFile(path: String): Unit = storage.rmFile(path)
  protected def openInputStream(path: String): InputStream = storage.openInputStream(path)
  protected def openOutputStream(path: String): OutputStream = storage.openOutputStream(path)
  protected def mv(from: String, to: String) = storage.mv(from, to)

  protected def upload(src: File, dest: String, options: TransferOptions) = {
    val os = if (!options.raw) openOutputStream(dest).toGZ else openOutputStream(dest)
    try src.copy(os, bufferSize, copyTimeout)
    finally timeout(os.close)(closeTimeout)
  }

  protected def download(src: String, dest: File, options: TransferOptions) = {
    val is = if (!options.raw) openInputStream(src).toGZ else openInputStream(src)
    try is.copy(dest, bufferSize, copyTimeout)
    finally timeout(is.close)(closeTimeout)
  }

  private def bufferSize = Workspace.preferenceAsInt(Storage.BufferSize)
  private def copyTimeout = Workspace.preferenceAsDuration(Storage.CopyTimeout)
  private def closeTimeout = Workspace.preferenceAsDuration(Storage.CloseTimeout)
}
