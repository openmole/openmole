package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.{ FileNode, TreeNodePanel }
import org.openmole.gui.ext.data._
import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.alert.AlertPanel

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.api.Api
import org.scalajs.dom

import scala.util.{ Failure, Success }
import com.raquo.laminar.api.L._

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

  def withTmpDirectory(todo: SafePath ⇒ Unit): Unit = {
    Fetch.future(_.temporaryDirectory(()).future).foreach { tempFile ⇒
      try todo(tempFile)
      finally Fetch.future(_.deleteFiles(Seq(tempFile), tempFile.context).future)
    }
  }

  def createDirectory(in: SafePath, dirName: String, onadded: () ⇒ Unit = () ⇒ {}) =
    Fetch.future(_.createDirectory(in, dirName).future).foreach { b ⇒
      if (b) onadded()
      else panels.alertPanel.string(s"$dirName already exists.", okaction = { () ⇒ {} }, transform = RelativeCenterPosition, zone = FileZone)
    }

  def createFile(safePath: SafePath, fileName: String, onadded: () ⇒ Unit = () ⇒ {}) =
    Fetch.future(_.createFile(safePath, fileName).future).foreach { b ⇒
      if (b) onadded()
      else panels.alertPanel.string(s" $fileName already exists.", okaction = { () ⇒ {} }, transform = RelativeCenterPosition, zone = FileZone)
    }

//  def trashNode(path: SafePath)(ontrashed: () ⇒ Unit): Unit = {
//    Post()[Api].deleteFiles(Seq(path), ServerFileSystemContext.project).call().foreach { d ⇒
//      panels.treeNodePanel.invalidCacheAnd(ontrashed)
//    }
//  }

  def trashNodes(paths: Seq[SafePath])(ontrashed: () ⇒ Unit): Unit = {
    Fetch.future(_.deleteFiles(paths, ServerFileSystemContext.project).future).foreach { d ⇒
      panels.treeNodePanel.invalidCacheAnd(ontrashed)
    }
  }

  def duplicate(safePath: SafePath, newName: String): Unit =
    Fetch.future(_.duplicate(safePath, newName).future).foreach { y ⇒
      panels.treeNodeManager.invalidCurrentCache
    }

//  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Future[Seq[SafePath]] =
//    Post()[Api].testExistenceAndCopyProjectFilesTo(safePaths, to).call()

  def copyProjectFiles(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean): Future[Seq[SafePath]] =
    Fetch.future(_.copyProjectFiles(safePaths, to, overwrite).future)

  def listFiles(safePath: SafePath, fileFilter: FileFilter = FileFilter()): Future[ListFilesData] = {
    Fetch.future(_.listFiles(safePath, fileFilter).future)
  }
  
 def findFilesContaining(safePath: SafePath, findString: Option[String]): Future[Seq[(SafePath, Boolean)]] = {
    Fetch.future(_.listRecursive(safePath, findString).future)
  }
 
  def appendToPluggedIfPlugin(safePath: SafePath) = {
//    Post()[Api].appendToPluggedIfPlugin(safePath).call().foreach { _ ⇒
//      panels.treeNodeManager.invalidCurrentCache
//      panels.pluginPanel.getPlugins
//    }
  }

  def addJSScript(relativeJSPath: String) = {
    org.scalajs.dom.document.body.appendChild(script(src := relativeJSPath).ref)
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

  case class ReadableByteCount(bytes: String, units: String) {
    def render = s"$bytes$units"
  }

  def dropDecimalIfNull(string: String) = {
    val cut = string.split('.')
    val avoid = Seq("0", "00", "000")
    if (avoid.contains(cut.last)) cut.head
    else string
  }

  //Duplicated from server to optimize data transfer
  def readableByteCount(bytes: Long): ReadableByteCount = {
    val kb = 1024L
    val mB = kb * kb
    val gB = mB * kb
    val tB = gB * kb

    val doubleBytes = bytes.toDouble
    if (bytes < mB) ReadableByteCount((doubleBytes / kb).formatted("%.2f").toString(), "KB")
    else if (bytes < gB) ReadableByteCount((doubleBytes / mB).formatted("%.2f").toString, "MB")
    else if (bytes < tB) ReadableByteCount((doubleBytes / gB).formatted("%.2f").toString, "GB")
    else ReadableByteCount((doubleBytes / tB).formatted("%.2f").toString, "TB")
  }

  def readableByteCountAsString(bytes: Long): String = readableByteCount(bytes).render

  def ifOrNothing(condition: Boolean, classString: String) = if (condition) classString else ""

  def setRoute(route: String) = dom.window.location.href = route.split("/").last



}
