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

import gridscale.local
import org.openmole.core.communication.storage._
import org.openmole.plugin.environment.batch.storage.StorageInterface
import org.openmole.tool.file._

object LogicalLinkStorage {

  import effectaside._

  implicit def isStorage: StorageInterface[LogicalLinkStorage] = new StorageInterface[LogicalLinkStorage] {
    implicit def interpreter = _root_.gridscale.local.Local()

    override def home(t: LogicalLinkStorage) = local.home
    override def child(t: LogicalLinkStorage, parent: String, child: String): String = (File(parent) / child).getAbsolutePath
    override def parent(t: LogicalLinkStorage, path: String): Option[String] = Option(File(path).getParent)
    override def name(t: LogicalLinkStorage, path: String): String = File(path).getName
    override def exists(t: LogicalLinkStorage, path: String): Boolean = local.exists(path)
    override def list(t: LogicalLinkStorage, path: String): Seq[gridscale.ListEntry] = local.list(path)
    override def makeDir(t: LogicalLinkStorage, path: String): Unit = local.makeDir(path)
    override def rmDir(t: LogicalLinkStorage, path: String): Unit = local.rmDir(path)
    override def rmFile(t: LogicalLinkStorage, path: String): Unit = local.rmFile(path)
    override def mv(t: LogicalLinkStorage, from: String, to: String): Unit = local.mv(from, to)

    override def upload(t: LogicalLinkStorage, src: File, dest: String, options: TransferOptions): Unit = {
      def copy = StorageInterface.upload(false, local.writeFile(_, _))(src, dest, options)

      if (options.canMove) mv(t, src.getPath, dest)
      else if (options.forceCopy || t.forceCopy) copy
      else local.link(src.getPath, dest) //new File(dest).createLinkTo(src)
    }
    override def download(t: LogicalLinkStorage, src: String, dest: File, options: TransferOptions): Unit = {
      def copy = StorageInterface.download(false, local.readFile[Unit](_, _))(src, dest, options)
      if (options.canMove) mv(t, src, dest.getPath)
      else if (options.forceCopy || t.forceCopy) copy
      else local.link(src, dest.getPath) //dest.createLinkTo(src)
    }
  }
}

case class LogicalLinkStorage(forceCopy: Boolean = false)

