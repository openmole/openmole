/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.fileservice

import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.{ Level, Logger }

import org.openmole.tool.file._

import scala.collection.immutable.HashSet
import scala.collection.mutable.WeakHashMap

object FileDeleter {

  private val cleanFiles = new LinkedBlockingQueue[File]
  private val deleters = new WeakHashMap[File, DeleteOnFinalize]

  private val cleanFilesThread = new Thread(new Runnable {

    override def run = {
      var finished = false

      while (!finished) {
        try cleanFiles.take.delete
        catch {
          case ex: InterruptedException ⇒
            Logger.getLogger(FileDeleter.getClass.getName).log(Level.INFO, "File deleter interrupted", ex)
            finished = true
        }
      }
    }
  })
  cleanFilesThread.setDaemon(true)
  cleanFilesThread.start

  def assynchonousRemove(file: File) = cleanFiles.add(file)

  def deleteWhenGarbageCollected(file: File): File = deleters.synchronized {
    deleters += file → new DeleteOnFinalize(file.getAbsolutePath)
    file
  }

  val cleanDirectories = new collection.mutable.HashSet[File]

  def deleteWhenEmpty(dir: File) = {
    if (dir.isDirectoryEmpty) dir.delete()
    else cleanDirectories.synchronized { cleanDirectories.add(dir) }
  }

}
