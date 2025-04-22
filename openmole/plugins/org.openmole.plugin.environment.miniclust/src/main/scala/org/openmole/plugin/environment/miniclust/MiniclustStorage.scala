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

object MiniclustStorage:
  def nodeInputPath(path: String) =
    import org.openmole.tool.hash.*
    val pathHash = Hash.string(path)
    s"__input__${pathHash}"

  class Remote extends RemoteStorage:
    override def upload(src: File, dest: Option[String], options: TransferOptions)(using TmpDirectory): String =
      dest.foreach: d =>
        val destFile = FileTools.homeDirectory / d
        if options.canMove
        then src.move(destFile)
        else src.copy(destFile)

      dest.getOrElse("")

    override def download(src: String, dest: File, options: TransferOptions)(using TmpDirectory): Unit =
      dest.createLinkTo((FileTools.homeDirectory / nodeInputPath(src)).getAbsoluteFile)

  def upload(baseDir: String, f: File, option: TransferOptions)(using _root_.gridscale.miniclust.Miniclust) =
    val path = s"$baseDir/${StorageSpace.timedUniqName}"
    _root_.gridscale.miniclust.upload(f, path)
    path

  def exists(f: String)(using _root_.gridscale.miniclust.Miniclust) = _root_.gridscale.miniclust.exists(f)
  def remove(f: String)(using _root_.gridscale.miniclust.Miniclust) = _root_.gridscale.miniclust.rmFile(f)
  def id(using mc: _root_.gridscale.miniclust.Miniclust) = s"${mc.bucket.server}-${mc.bucket.name}"


//  given StorageInterface[MiniclustStorage] with HierarchicalStorageInterface[MiniclustStorage] with EnvironmentStorage[MiniclustStorage]:
//    override def exists(t: MiniclustStorage, path: String)(using Priority): Boolean = MiniclustStorage.exists(path)(using t.miniclust)
//    override def rmFile(t: MiniclustStorage, path: String)(using Priority): Unit = MiniclustStorage.remove(path)(using t.miniclust)
//
//    override def upload(t: MiniclustStorage, src: File, dest: String, options: TransferOptions)(using Priority): Unit = ???
//
//    override def download(t: MiniclustStorage, src: String, dest: File, options: TransferOptions)(using Priority): Unit = ???
//
//    override def rmDir(t: MiniclustStorage, path: String)(using Priority): Unit = ???
//
//    override def makeDir(t: MiniclustStorage, path: String)(using Priority): Unit = ???
//
//    override def child(t: MiniclustStorage, parent: String, child: String)(using Priority): String = ???
//
//    override def list(t: MiniclustStorage, path: String)(using Priority): Seq[Any] = ???
//
//    override def parent(t: MiniclustStorage, path: String)(using Priority): Option[String] = ???
//
//    override def name(t: MiniclustStorage, path: String): String = ???
//
//    override def id(s: MiniclustStorage): String = ???
//
//    override def environment(s: MiniclustStorage): BatchEnvironment = ???
//
//
//case class MiniclustStorage(miniclust: _root_.gridscale.miniclust.Miniclust)