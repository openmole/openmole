package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.{ TreeNode, DirNode }
import org.openmole.gui.ext.data._
import org.openmole.gui.shared.Api
import autowire._
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }

/*
 * Copyright (C) 22/12/15 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object CoreUtils {

  implicit class SeqUpdater[T](sequence: Seq[T]) {
    def updatedFirst(t: T, s: T): Seq[T] = {
      val index = sequence.indexOf(t)
      if (index != -1) sequence.updated(index, s)
      else sequence
    }

    def updatedFirst(cond: T ⇒ Boolean, s: T): Seq[T] =
      sequence.find(cond).map { e ⇒ updatedFirst(e, s) }.getOrElse(sequence)
  }

  def withTmpFile(todo: SafePath ⇒ Unit): Unit = {
    OMPost[Api].temporaryFile.call().foreach { tempFile ⇒
      todo(tempFile)
    }
  }

  def refreshCurrentDirectory(todo: () ⇒ Unit = () ⇒ {}, fileFilter: FileFilter) = manager.trashCache(todo, fileFilter)

  def addDirectory(in: TreeNodeData, dirName: String, onadded: () ⇒ Unit = () ⇒ {}) =
    OMPost[Api].addDirectory(in, dirName).call().foreach { b ⇒
      if (b) onadded
    }

  def addFile(in: TreeNodeData, fileName: String, onadded: () ⇒ Unit = () ⇒ {}) =
    OMPost[Api].addFile(in, fileName).call().foreach { b ⇒
      if (b) onadded()
    }

  def trashNode(path: SafePath, fileFilter: FileFilter)(ontrashed: () ⇒ Unit): Unit = {
    OMPost[Api].deleteFile(path, ServerFileSytemContext.project).call().foreach { d ⇒
      refreshAndSwitchSelection(fileFilter, ontrashed)
    }
  }

  def trashNodes(paths: Seq[SafePath], fileFilter: FileFilter)(ontrashed: () ⇒ Unit): Unit = {
    OMPost[Api].deleteFiles(paths, ServerFileSytemContext.project).call().foreach { d ⇒
      refreshAndSwitchSelection(fileFilter, ontrashed)
    }
  }

  def replicate(treeNode: TreeNode, onreplicated: (TreeNodeData) ⇒ Unit) = {
    OMPost[Api].replicate(treeNode).call().foreach { r ⇒
      onreplicated(r)
    }
  }

  def refreshAndSwitchSelection(fileFilter: FileFilter, onrefreshed: () ⇒ Unit = () ⇒ {}) = {
    refreshCurrentDirectory(onrefreshed, fileFilter)
    manager.switchOffSelection
  }

  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Future[Seq[SafePath]] =
    OMPost[Api].testExistenceAndCopyProjectFilesTo(safePaths, to).call()

  def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Future[Unit] =
    OMPost[Api].copyProjectFilesTo(safePaths, to).call()

  def updateSons(dirNode: DirNode, todo: () ⇒ Unit = () ⇒ {}, fileFilter: FileFilter) = {

    OMPost[Api].listFiles(dirNode.safePath(), fileFilter).call().foreach { s ⇒
      manager.updateSon(dirNode, s)
      todo()
    }
  }
}
