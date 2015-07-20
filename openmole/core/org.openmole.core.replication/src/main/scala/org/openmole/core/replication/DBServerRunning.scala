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
package org.openmole.core.replication

import java.io.{ FileOutputStream, File }
import java.util.UUID

object DBServerRunning {

  def newRunningFile = {
    val f = new File(runningDirectory, UUID.randomUUID().toString)
    f.createNewFile
    f.deleteOnExit()
    f
  }

  def runningDirectory = {
    val dir = new File(DBServerInfo.dbDirectory, "running")
    dir.mkdirs()
    dir
  }

  def useDB[T](f: ⇒ T): T = {
    val locked = newRunningFile
    val os = new FileOutputStream(locked)
    try {
      val lock = os.getChannel.lock()
      try f
      finally lock.release()
    }
    finally {
      os.close
      locked.delete()
    }
  }

  def oneLocked =
    runningDirectory.listFiles.exists {
      f ⇒
        val os = new FileOutputStream(f)
        try {
          val lock = os.getChannel.tryLock()
          if (lock == null) true else { lock.release(); false }
        }
        finally os.close
    }

  def cleanRunning = runningDirectory.listFiles().foreach(_.delete)

}
