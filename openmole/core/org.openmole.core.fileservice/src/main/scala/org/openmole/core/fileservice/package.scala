/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.core

import java.io.File

import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.tool.file._
import org.openmole.tool.hash._
import squants.time.Time

import scala.util.{ Failure, Success, Try }

package object fileservice {

  def lockFile(f: File) = {
    val lock = new File(f.getPath + "-lock")
    lock.createNewFile()
    lock
  }

  implicit class FileServiceDecorator(file: File) {

    def cache(get: File => Unit): File = {
      lockFile(file).withLock { _ =>
        if (!file.exists())
          try get(file)
          catch {
            case t: Throwable =>
              file.delete()
              throw t
          }
      }
      file
    }

    def updateIfTooOld(tooOld: Time)(update: File => Unit) = {
      def timeStamp(f: File) = new File(f.getPath + "-timestamp")
      lockFile(file).withLock { _ =>
        val ts = timeStamp(file)
        val upToDate =
          if (!file.exists || !ts.exists) false
          else
            Try(ts.content.toLong) match {
              case Success(v) => v + tooOld.millis > System.currentTimeMillis
              case Failure(_) => ts.delete; false
            }

        if (!upToDate) {
          update(file)
          ts.content = System.currentTimeMillis.toString
        }
      }
      file
    }

  }
}
