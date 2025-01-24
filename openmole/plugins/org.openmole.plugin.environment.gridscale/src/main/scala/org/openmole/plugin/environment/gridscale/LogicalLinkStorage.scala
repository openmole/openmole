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
package org.openmole.plugin.environment.gridscale

import gridscale.local.Local
import org.openmole.core.communication.storage._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.environment.batch.storage.{ StorageInterface, StorageSpace }
import org.openmole.tool.file._

object LogicalLinkStorage {

  def child(t: LogicalLinkStorage, parent: String, child: String): String = (File(parent) / child).getAbsolutePath
  def parent(t: LogicalLinkStorage, path: String): Option[String] = Option(File(path).getParent)
  def name(t: LogicalLinkStorage, path: String): String = File(path).getName
  def exists(t: LogicalLinkStorage, path: String): Boolean = Local.exists(path)
  def list(t: LogicalLinkStorage, path: String): Seq[gridscale.ListEntry] = Local.list(path)
  def makeDir(t: LogicalLinkStorage, path: String): Unit = Local.makeDir(path)
  def rmDir(t: LogicalLinkStorage, path: String): Unit = Local.rmDir(path)
  def rmFile(t: LogicalLinkStorage, path: String): Unit = Local.rmFile(path)

  def upload(t: LogicalLinkStorage, src: File, dest: String, options: TransferOptions): Unit =
    def copy = StorageInterface.upload(false, Local.writeFile)(src, dest, options)

    if options.canMove then Local.mv(src.getPath, dest)
    else
      if options.noLink || t.forceCopy
      then copy
      else Local.link(src.getPath, dest) //new File(dest).createLinkTo(src)

  def download(t: LogicalLinkStorage, src: String, dest: File, options: TransferOptions): Unit =
    def copy = StorageInterface.download(false, Local.readFile)(src, dest, options)
    if options.canMove
    then Local.mv(src, dest.getPath)
    else
      if options.noLink || t.forceCopy
      then copy
      else Local.link(src, dest.getPath) //dest.createLinkTo(src)

  def remote(s: LogicalLinkStorage, jobDirectory: String) =
    new RemoteStorage:
      override def upload(src: File, dest: Option[String], options: TransferOptions)(implicit newFile: TmpDirectory): String =
        val uploadDestination = dest.getOrElse(LogicalLinkStorage.child(s, jobDirectory, StorageSpace.timedUniqName))
        LogicalLinkStorage.upload(s, src, uploadDestination, options)
        uploadDestination

      override def download(src: String, dest: File, options: TransferOptions)(implicit newFile: TmpDirectory): Unit =
        LogicalLinkStorage.download(s, src, dest, options)


}

case class LogicalLinkStorage(forceCopy: Boolean = false)

