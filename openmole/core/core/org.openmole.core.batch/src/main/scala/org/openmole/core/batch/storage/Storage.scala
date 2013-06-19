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

import fr.iscpif.gridscale.{ Storage ⇒ GSStorage }
import fr.iscpif.gridscale.FileType
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.workspace._

object Storage {
  val Timeout = new ConfigurationLocation("Storage", "Timeout")
  val BufferSize = new ConfigurationLocation("Storage", "BufferSize")
  val CopyTimeout = new ConfigurationLocation("Storage", "CopyTimeout")

  Workspace += (Timeout, "PT2M")
  Workspace += (BufferSize, "8192")
  Workspace += (CopyTimeout, "PT1M")

  def uniqName(prefix: String, sufix: String) = prefix + "_" + UUID.randomUUID.toString + sufix
}

trait Storage {

  val storage: GSStorage
  def authentication: storage.A

  def root: String

  def child(parent: String, child: String) = storage.child(parent, child)

  protected def exists(path: String): Boolean = storage.exists(path)(authentication)
  protected def listNames(path: String): Seq[String] = storage.listNames(path)(authentication)
  protected def list(path: String): Seq[(String, FileType)] = storage.list(path)(authentication)
  protected def makeDir(path: String): Unit = storage.makeDir(path)(authentication)
  protected def rmDir(path: String): Unit = storage.rmDir(path)(authentication)
  protected def rmFile(path: String): Unit = storage.rmFile(path)(authentication)
  protected def openInputStream(path: String): InputStream = storage.openInputStream(path)(authentication)
  protected def openOutputStream(path: String): OutputStream = storage.openOutputStream(path)(authentication)
  protected def mv(from: String, to: String) = storage.mv(from, to)(authentication)

  protected def upload(src: File, dest: String) = {
    val os = openOutputStream(dest)
    try src.copy(os, bufferSize, copyTimeout)
    finally os.close
  }

  protected def uploadGZ(src: File, dest: String) = {
    val os = openOutputStream(dest).toGZ
    try src.copy(os, bufferSize, copyTimeout)
    finally os.close
  }

  protected def download(src: String, dest: File) = {
    val is = openInputStream(src)
    try is.copy(dest, bufferSize, copyTimeout)
    finally is.close
  }

  protected def downloadGZ(src: String, dest: File) = {
    val is = openInputStream(src).toGZ
    try is.copy(dest, bufferSize, copyTimeout)
    finally is.close
  }

  private def bufferSize = Workspace.preferenceAsInt(Storage.BufferSize)
  private def copyTimeout = Workspace.preferenceAsDuration(Storage.CopyTimeout).toMilliSeconds
}
