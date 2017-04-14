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
package org.openmole.core.db

import java.io.{ File, FileOutputStream }
import java.util.UUID

import org.openmole.tool.logger._
import org.openmole.tool.file._
import org.openmole.tool.network._

import scala.annotation.tailrec
import scala.util._

object DBServerRunning extends Logger {

  def newRunningFile(dbDirectory: File) = {
    val f = new File(runningDirectory(dbDirectory), UUID.randomUUID().toString)
    f.createNewFile
    f.deleteOnExit()
    f
  }

  def runningDirectory(dbDirectory: File) = {
    val dir = new File(dbDirectory, "running")
    dir.mkdirs()
    dir
  }

  def useDB[T](dbDirectory: File)(f: DBServerInfo ⇒ T): T = {

    @tailrec def waitDBInfo(): DBServerInfo = {
      def portIsOpened(dBServerInfo: DBServerInfo) = isPortAcceptingConnections("localhost", dBServerInfo.port)
      def sleep = Thread.sleep(100)

      if (dbInfoFile(dbDirectory).exists() && !dbInfoFile(dbDirectory).isEmpty)
        Try(load(dbInfoFile(dbDirectory))) match {
          case Success(info) ⇒
            if (!portIsOpened(info)) {
              Log.logger.info(s"Database server is not accepting connection on port ${info.port}, waiting...")
              sleep
              waitDBInfo()
            }
            else info
          case Failure(_) ⇒
            Log.logger.info(s"Failed to deserialize database server information file ${dbInfoFile(dbDirectory)}, waiting...")
            sleep
            waitDBInfo()
        }
      else {
        Log.logger.info(s"Database server information file ${dbInfoFile(dbDirectory)} does'nt exist or is empty, waiting...")
        sleep
        waitDBInfo()
      }
    }

    val dbInfo = waitDBInfo()

    val locked = newRunningFile(dbDirectory)
    val os = new FileOutputStream(locked)
    try {
      val lock = os.getChannel.lock()
      try f(dbInfo)
      finally lock.release()
    }
    finally {
      os.close
      locked.delete()
    }
  }

  def oneLocked(dbDirectory: File) =
    runningDirectory(dbDirectory).listFiles.exists {
      f ⇒
        val os = new FileOutputStream(f)
        try {
          val lock = os.getChannel.tryLock()
          if (lock == null) true else { lock.release(); false }
        }
        finally os.close
    }

  def cleanRunning(dbDirectory: File) = runningDirectory(dbDirectory).listFiles().foreach(_.delete)

}
