/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.batch.storage

import java.io.File
import java.net.URI
import org.openmole.core.batch.control._
import org.openmole.core.batch.replication._
import org.openmole.core.tools.service.{ Logger, ThreadUtil }
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import fr.iscpif.gridscale.storage._
import scala.concurrent.duration.Duration
import ThreadUtil._

import scala.slick.driver.H2Driver.simple._

object PersistentStorageService extends Logger {

  val TmpDirRemoval = new ConfigurationLocation("StorageService", "TmpDirRemoval")
  Workspace += (TmpDirRemoval, "P30D")

  val persistent = "persistent/"
  val tmp = "tmp/"

}

trait PersistentStorageService extends StorageService {

  import PersistentStorageService._

  override def persistentDir(implicit token: AccessToken, session: Session): String =
    directoryCache.get(
      "persistentDir",
      () ⇒ createPersistentDir
    )

  private def createPersistentDir(implicit token: AccessToken, session: Session) = {
    val persistentPath = child(baseDir, persistent)
    if (!super.exists(persistentPath)) super.makeDir(persistentPath)

    def graceIsOver(name: String) =
      ReplicaCatalog.timeOfPersistent(name).map {
        _ + Workspace.preferenceAsDuration(ReplicaCatalog.ReplicaGraceTime).toMillis < System.currentTimeMillis
      }.getOrElse(true)

    for {
      name ← super.listNames(persistentPath)
      if graceIsOver(name)
    } {
      val path = super.child(persistentPath, name)
      if (!ReplicaCatalog.forPath(path).exists.run) backgroundRmFile(path)
    }
    persistentPath
  }

  override def tmpDir(implicit token: AccessToken) =
    directoryCache.get(
      "tmpDir",
      () ⇒ createTmpDir
    )

  private def createTmpDir(implicit token: AccessToken) = {
    val time = System.currentTimeMillis

    val tmpNoTime = child(baseDir, tmp)
    if (!super.exists(tmpNoTime)) super.makeDir(tmpNoTime)

    val removalDate = System.currentTimeMillis - Workspace.preferenceAsDuration(TmpDirRemoval).toMillis

    for ((name, fileType) ← super.list(tmpNoTime)) {
      val childPath = child(tmpNoTime, name)

      def rmDir =
        try {
          val timeOfDir = (if (name.endsWith("/")) name.substring(0, name.length - 1) else name).toLong
          if (timeOfDir < removalDate) backgroundRmDir(childPath)
        }
        catch {
          case (ex: NumberFormatException) ⇒ backgroundRmDir(childPath)
        }

      fileType match {
        case DirectoryType ⇒ rmDir
        case FileType      ⇒ backgroundRmFile(childPath)
        case LinkType      ⇒ backgroundRmFile(childPath)
        case UnknownType ⇒
          try rmDir
          catch {
            case e: Throwable ⇒ backgroundRmFile(childPath)
          }
      }
    }

    val tmpTimePath = super.child(tmpNoTime, time.toString)
    if (!super.exists(tmpTimePath)) super.makeDir(tmpTimePath)
    tmpTimePath
  }

}
