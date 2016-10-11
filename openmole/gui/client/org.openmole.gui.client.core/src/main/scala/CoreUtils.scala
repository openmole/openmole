package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.TreeNodePanel
import org.openmole.gui.ext.data._
import org.openmole.gui.shared.Api
import autowire._
import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.alert.AlertPanel

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

  def addDirectory(in: SafePath, dirName: String, onadded: () ⇒ Unit = () ⇒ {}) =
    OMPost[Api].addDirectory(in, dirName).call().foreach { b ⇒
      if (b) onadded()
      else AlertPanel.string(s"$dirName already exists.", okaction = { () ⇒ {} }, transform = RelativeCenterPosition, zone = FileZone)

    }

  def addFile(safePath: SafePath, fileName: String, onadded: () ⇒ Unit = () ⇒ {}) =
    OMPost[Api].addFile(safePath, fileName).call().foreach { b ⇒
      if (b) onadded()
      else AlertPanel.string(s" $fileName already exists.", okaction = { () ⇒ {} }, transform = RelativeCenterPosition, zone = FileZone)
    }

  def trashNode(path: SafePath)(ontrashed: () ⇒ Unit): Unit = {
    OMPost[Api].deleteFile(path, ServerFileSytemContext.project).call().foreach { d ⇒
      TreeNodePanel.refreshAnd(ontrashed)
    }
  }

  def trashNodes(paths: Seq[SafePath])(ontrashed: () ⇒ Unit): Unit = {
    OMPost[Api].deleteFiles(paths, ServerFileSytemContext.project).call().foreach { d ⇒
      TreeNodePanel.refreshAnd(ontrashed)
    }
  }

  def replicate(safePath: SafePath, onreplicated: (SafePath) ⇒ Unit) = {
    OMPost[Api].replicate(safePath).call().foreach { r ⇒
      onreplicated(r)
    }
  }

  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Future[Seq[SafePath]] =
    OMPost[Api].testExistenceAndCopyProjectFilesTo(safePaths, to).call()

  def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Future[Unit] =
    OMPost[Api].copyProjectFilesTo(safePaths, to).call()

  def getSons(safePath: SafePath, fileFilter: FileFilter): Future[Seq[TreeNodeData]] =
    OMPost[Api].listFiles(safePath, fileFilter).call()

  def updateSons(safePath: SafePath, todo: () ⇒ Unit = () ⇒ {}, fileFilter: FileFilter) = {
    getSons(safePath, fileFilter).foreach { s ⇒
      manager.updateSon(safePath, s)
      todo()
    }
  }

  def pluggables(safePath: SafePath, todo: () ⇒ Unit) = OMPost[Api].allPluggableIn(safePath).call.foreach { p ⇒
    manager.pluggables() = p
    todo()
  }

  def approximatedYearMonthDay(duration: Long): String = {
    val MINUTE = 60000L
    val HOUR = 60L * MINUTE
    val DAY = 24L * HOUR
    val MONTH = 30L * DAY
    val YEAR = 12L * MONTH

    val y = duration / YEAR
    val restInYear = duration - (y * YEAR)

    val m = restInYear / MONTH
    val restInMonth = restInYear - (m * MONTH)

    val d = restInMonth / DAY
    val restInDay = restInMonth - (d * DAY)

    val h = restInDay / HOUR

    s"$y y $m m $d d $h h"
  }

  def longTimeToString(lg: Long): String = {
    val date = new scalajs.js.Date(lg)
    s"${date.toLocaleDateString} ${date.toLocaleTimeString.dropRight(3)}"
  }

  //Duplicated from server to optimize data transfer
  def readableByteCount(bytes: Long): String = {
    val kb = 1024L
    val mB = kb * kb
    val gB = mB * kb
    val tB = gB * kb

    val doubleBytes = bytes.toDouble
    if (bytes < mB) (doubleBytes / kb).formatted("%.2f").toString() + "KB"
    else if (bytes < gB) (doubleBytes / mB).formatted("%.2f").toString + "MB"
    else if (bytes < tB) (doubleBytes / gB).formatted("%.2f").toString + "GB"
    else (doubleBytes / tB).formatted("%.2f").toString + "TB"
  }

}
