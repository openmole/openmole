/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.misc.filedeleter.internal

import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.misc.filedeleter.IFileDeleter
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap


class FileDeleter extends IFileDeleter {

  val cleanFiles = new LinkedBlockingQueue[File]
  val deleters = new WeakHashMap[File, DeleteOnFinalize] with SynchronizedMap[File, DeleteOnFinalize]

  val thread = new Thread(new Runnable {

      override def run = {
        var finished = false

        while (!finished) {
          try {
            cleanFiles.take.delete
          } catch  {
            case ex: InterruptedException => 
              Logger.getLogger(classOf[FileDeleter].getName).log(Level.INFO, "File deleter interupted", ex)
              finished = true
          }
        }
      }
    })
  thread.setDaemon(true)
  thread.start
    
  override def assynchonousRemove(file: File) = cleanFiles.add(file)

  override def deleteWhenGarbageCollected(file: File) = deleters += file -> new DeleteOnFinalize(file.getAbsolutePath)
}
