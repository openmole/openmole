package org.openmole.plugin.environment.miniclust

import org.openmole.core.communication.storage.*
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.environment.batch.environment.*
import org.openmole.tool.file.*
import org.openmole.core.communication.message.*
import org.openmole.plugin.environment.batch.environment.AccessControl.Priority
import org.openmole.plugin.environment.batch.storage.*

import java.io.File

/*
 * Copyright (C) 2025 Romain Reuillon
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

object MiniClustStorage:
  def nodeInputPath(path: String) =
    import org.openmole.tool.hash.*
    val pathHash = Hash.string(path)
    s"__input__${pathHash}"

  class Remote extends RemoteStorage:
    override def upload(src: File, dest: Option[String], options: TransferOptions)(using TmpDirectory): String =
      val uploadDestination = FileTools.homeDirectory / dest.getOrElse(StorageSpace.timedUniqName)

      if options.canMove
      then src.move(uploadDestination)
      else src.copy(uploadDestination)

      uploadDestination.getName

    override def download(src: String, dest: File, options: TransferOptions)(using TmpDirectory): Unit =
      dest.createLinkTo((FileTools.homeDirectory / nodeInputPath(src)).getAbsoluteFile)


  private def exists(f: String)(using _root_.gridscale.miniclust.Miniclust) = _root_.gridscale.miniclust.exists(f)
  private def remove(f: String)(using _root_.gridscale.miniclust.Miniclust) = _root_.gridscale.miniclust.rmFile(f)
  private def id(using mc: _root_.gridscale.miniclust.Miniclust) = s"${mc.minio.server}-${mc.bucket.name}"

  given HierarchicalStorageInterface[MiniClustStorage]:
    override def exists(t: MiniClustStorage, path: String)(using Priority): Boolean = t.accessControl:
      _root_.gridscale.miniclust.exists(path)(using t.miniclust)

    override def rmFile(t: MiniClustStorage, path: String)(using Priority): Unit = t.accessControl:
      _root_.gridscale.miniclust.rmFile(path)(using t.miniclust)

    override def upload(t: MiniClustStorage, src: File, dest: String, options: TransferOptions)(using Priority): Unit = t.accessControl:
      _root_.gridscale.miniclust.upload(src, dest)(using t.miniclust)

    override def download(t: MiniClustStorage, src: String, dest: File, options: TransferOptions)(using Priority): Unit = t.accessControl:
      _root_.gridscale.miniclust.download(src, dest)(using t.miniclust)

    override def rmDir(t: MiniClustStorage, path: String)(using Priority): Unit = t.accessControl:
      _root_.gridscale.miniclust.rmDir(path)(using t.miniclust)

    override def makeDir(t: MiniClustStorage, path: String)(using Priority): Unit = ()
    override def child(t: MiniClustStorage, parent: String, child: String)(using Priority): String = _root_.gridscale.RemotePath.child(parent, child)

    override def list(t: MiniClustStorage, path: String)(using Priority) = t.accessControl:
      _root_.gridscale.miniclust.list(path)(using t.miniclust)

    override def parent(t: MiniClustStorage, path: String)(using Priority): Option[String] = gridscale.RemotePath.parent(path)
    override def name(t: MiniClustStorage, path: String): String = gridscale.RemotePath.name(path)
    override def id(s: MiniClustStorage): String = MiniClustStorage.id(using s.miniclust)

case class MiniClustStorage(miniclust: _root_.gridscale.miniclust.Miniclust, accessControl: AccessControl)