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

  import freedsl.dsl._

  implicit def isStorage(implicit interpreter: local.LocalInterpreter): StorageInterface[LogicalLinkStorage] = new StorageInterface[LogicalLinkStorage] {
    override def home(t: LogicalLinkStorage) = local.home[DSL].eval
    override def child(t: LogicalLinkStorage, parent: String, child: String): String = (File(parent) / child).getAbsolutePath
    override def parent(t: LogicalLinkStorage, path: String): Option[String] = Option(File(path).getParent)
    override def name(t: LogicalLinkStorage, path: String): String = File(path).getName
    override def exists(t: LogicalLinkStorage, path: String): Boolean = local.exists[DSL](path).eval
    override def list(t: LogicalLinkStorage, path: String): Seq[gridscale.ListEntry] = local.list[DSL](path).eval
    override def makeDir(t: LogicalLinkStorage, path: String): Unit = local.makeDir[DSL](path).eval
    override def rmDir(t: LogicalLinkStorage, path: String): Unit = local.rmDir[DSL](path).eval
    override def rmFile(t: LogicalLinkStorage, path: String): Unit = local.rmFile[DSL](path).eval
    override def mv(t: LogicalLinkStorage, from: String, to: String): Unit = local.mv[DSL](from, to).eval

    override def upload(t: LogicalLinkStorage, src: File, dest: String, options: TransferOptions): Unit = {
      def copy = StorageInterface.upload(false, local.writeFile[DSL](_, _).eval)(src, dest, options)
      if (options.canMove) mv(t, src.getPath, dest)
      else if (options.forceCopy) copy
      else local.link[DSL](src.getPath, dest).eval //new File(dest).createLinkTo(src)
    }
    override def download(t: LogicalLinkStorage, src: String, dest: File, options: TransferOptions): Unit = {
      def copy = StorageInterface.download(false, local.readFile[DSL, Unit](_, _).eval)(src, dest, options)

      if (options.canMove) mv(t, src, dest.getPath)
      else if (options.forceCopy) copy
      else local.link[DSL](src, dest.getPath).eval //dest.createLinkTo(src)
    }
  }
}

case class LogicalLinkStorage()
//
//object LogicalLinkStorage {
//  def apply(root: String) = {
//    val _root = root
//    new LogicalLinkStorage {
//      val root = _root
//    }
//  }
//}
//
//trait LogicalLinkStorage extends LocalStorage {
//
//  override protected def _upload(src: File, dest: String, options: TransferOptions): Unit = {
//    if (options.canMove) _mv(src.getPath, dest)
//    else if (options.forceCopy) super._upload(src, dest, options)
//    else new File(dest).createLinkTo(src)
//  }
//
//  override protected def _download(src: String, dest: File, options: TransferOptions): Unit = {
//    if (options.canMove) _mv(src, dest.getPath)
//    else if (options.forceCopy) super._download(src, dest, options)
//    else dest.createLinkTo(src)
//  }
//
//}
