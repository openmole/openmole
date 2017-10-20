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
import org.openmole.core.threadprovider.Updater
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.tool.file._
import org.openmole.tool.stream._
import simulacrum.typeclass
import squants.time.TimeConversions._
import squants.information.InformationConversions._

import scala.ref.WeakReference

//object Storage {
//  val BufferSize = ConfigurationLocation("Storage", "BufferSize", Some(64 kilobytes))
//  val CopyTimeout = ConfigurationLocation("Storage", "CopyTimeout", Some(1 minutes))
//  val CloseTimeout = ConfigurationLocation("Storage", "CloseTimeout", Some(1 minutes))
//}
//
//trait CompressedTransfer <: Storage {
//
//  override abstract def _uploadStream(src: InputStream, dest: String, options: TransferOptions) =
//    if (!options.raw) super._uploadStream(src.toGZiped, dest, options) else super._uploadStream(src, dest, options)
//
//  override abstract protected def _downloadStream(src: String, options: TransferOptions) =
//    if (!options.raw) super._downloadStream(src, options).toGZ else super._downloadStream(src, options)
//
//}
//

object StorageInterface {

  def remote[S](s: S)(implicit storage: StorageInterface[S]) =
    new RemoteStorage {
      override def upload(src: File, dest: String, options: TransferOptions)(implicit newFile: NewFile): Unit = storage.upload(s, src, dest, options)
      override def download(src: String, dest: File, options: TransferOptions)(implicit newFile: NewFile): Unit = storage.download(s, src, dest, options)
      override def child(parent: String, child: String): String = storage.child(s, parent, child)
    }

  def upload(compressed: Boolean, uploadStream: (() ⇒ InputStream, String) ⇒ Unit)(src: File, dest: String, options: TransferOptions = TransferOptions.default): Unit = {
    def fileStream() = src.bufferedInputStream

    if (compressed) {
      def compressedFileStream() = src.bufferedInputStream.toGZiped
      if (!options.raw) uploadStream(compressedFileStream, dest) else uploadStream(fileStream, dest)
    }
    else uploadStream(fileStream, dest)
  }

  def download(compressed: Boolean, downloadStream: (String, InputStream ⇒ Unit) ⇒ Unit)(src: String, dest: File, options: TransferOptions = TransferOptions.default): Unit = {
    def downloadFile(is: InputStream) = Files.copy(is, dest.toPath)
    if (compressed) {
      def uncompressed(is: InputStream) = downloadFile(is.toGZ)
      if (!options.raw) downloadStream(src, uncompressed) else downloadStream(src, downloadFile)
    }
    else downloadStream(src, downloadFile)
  }

}

@typeclass trait StorageInterface[T] {
  def home(t: T): String
  def child(t: T, parent: String, child: String): String
  def parent(t: T, path: String): Option[String]
  def name(t: T, path: String): String

  def exists(t: T, path: String): Boolean
  // def listNames(t: T, path: String): Seq[String]
  def list(t: T, path: String): Seq[ListEntry]
  def makeDir(t: T, path: String): Unit
  def rmDir(t: T, path: String): Unit
  def rmFile(t: T, path: String): Unit
  def mv(t: T, from: String, to: String)

  def upload(t: T, src: File, dest: String, options: TransferOptions = TransferOptions.default): Unit
  def download(t: T, src: String, dest: File, options: TransferOptions = TransferOptions.default): Unit
}
