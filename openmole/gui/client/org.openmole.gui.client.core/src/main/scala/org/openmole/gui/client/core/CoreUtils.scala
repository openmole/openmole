package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.*
import org.openmole.gui.shared.data.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom

import scala.util.{Failure, Success}
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

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

  implicit class SeqUpdater[T](sequence: Seq[T]):
    def updatedFirst(t: T, s: T): Seq[T] = {
      val index = sequence.indexOf(t)
      if (index != -1) sequence.updated(index, s)
      else sequence
    }

    def updatedFirst(cond: T => Boolean, s: T): Seq[T] =
      sequence.find(cond).map { e => updatedFirst(e, s) }.getOrElse(sequence)

  
  def createFile(safePath: SafePath, fileName: String, directory: Boolean = false)(using panels: Panels, api: ServerAPI, path: BasePath): Future[Unit] =
    api.createFile(safePath, fileName, directory).flatMap { b =>
      if b
      then Future.successful(())
      else
        panels.notifications.showGetItNotification(NotificationLevel.Error, s" $fileName already exists.")
        Future.failed(new java.io.IOException(s"File already $fileName exists in ${safePath.path.mkString}"))
    }
  
  def trashNodes(treeNodePanel: TreeNodePanel, paths: Seq[SafePath])(using api: ServerAPI, path: BasePath): Future[Unit] =
    api.deleteFiles(paths).andThen { _ => treeNodePanel.refresh }

  def listFiles(safePath: SafePath, fileFilter: FileSorting = FileSorting(), withHidden: Boolean = true)(using api: ServerAPI, path: BasePath): Future[FileListData] = api.listFiles(safePath, fileFilter, withHidden)
  
  def findFilesContaining(safePath: SafePath, findString: Option[String])(using api: ServerAPI, path: BasePath): Future[Seq[(SafePath, Boolean)]] = api.listRecursive(safePath, findString)
  
  def addPlugin(safePath: SafePath)(using api: ServerAPI, path: BasePath) = api.addPlugin(safePath)
  def removePlugin(safePath: SafePath)(using api: ServerAPI, path: BasePath) = api.removePlugin(safePath)

  def addJSScript(relativeJSPath: String) = 
    org.scalajs.dom.document.body.appendChild(scriptTag(src := relativeJSPath).ref)

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

  def longTimeToString(lg: Long): String =
    val date = new scalajs.js.Date(lg)
    s"${date.toLocaleDateString} ${date.toLocaleTimeString.dropRight(3)}"

  case class ReadableByteCount(bytes: String, units: String):
    def render = s"$bytes$units"


  def dropDecimalIfNull(string: String) =
    val cut = string.split('.')
    val avoid = Seq("0", "00", "000")
    if (avoid.contains(cut.last)) cut.head
    else string

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
