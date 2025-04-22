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
package org.openmole.plugin.environment.batch.storage

import java.io.{ ByteArrayInputStream, File, InputStream }
import java.nio.file.Files

import gridscale._
import org.openmole.core.communication.storage._
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, AccessControl }
import org.openmole.tool.file._
import org.openmole.tool.stream._

object StorageInterface:

  def upload(compressed: Boolean, uploadStream: (() => InputStream, String) => Unit)(src: File, dest: String, options: TransferOptions = TransferOptions.default): Unit =
    def fileStream() = src.bufferedInputStream()

    if compressed
    then
      def compressedFileStream() = src.bufferedInputStream().toGZiped
      if (!options.raw) uploadStream(compressedFileStream, dest) else uploadStream(fileStream, dest)
    else uploadStream(fileStream, dest)

  def download(compressed: Boolean, downloadStream: (String, InputStream => Unit) => Unit)(src: String, dest: File, options: TransferOptions = TransferOptions.default): Unit =
    def downloadFile(is: InputStream) = Files.copy(is, dest.toPath)
    if compressed
    then
      def uncompressed(is: InputStream) = downloadFile(is.toGZ)
      if (!options.raw) downloadStream(src, uncompressed) else downloadStream(src, downloadFile)
    else downloadStream(src, downloadFile)

  def isDirectory(name: String) = name.endsWith("/")


trait StorageInterface[T]:
  def exists(t: T, path: String)(using AccessControl.Priority): Boolean
  def rmFile(t: T, path: String)(using AccessControl.Priority): Unit
  def upload(t: T, src: File, dest: String, options: TransferOptions = TransferOptions.default)(using AccessControl.Priority): Unit
  def download(t: T, src: String, dest: File, options: TransferOptions = TransferOptions.default)(using AccessControl.Priority): Unit

trait HierarchicalStorageInterface[T] extends StorageInterface[T]:
  def rmDir(t: T, path: String)(using AccessControl.Priority): Unit
  def makeDir(t: T, path: String)(using AccessControl.Priority): Unit
  def child(t: T, parent: String, child: String)(using AccessControl.Priority): String
  def list(t: T, path: String)(using AccessControl.Priority): Seq[ListEntry]
  def parent(t: T, path: String)(using AccessControl.Priority): Option[String]
  def name(t: T, path: String): String
  def id(s: T): String