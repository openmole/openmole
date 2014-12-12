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
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace._
import fr.iscpif.gridscale.storage._
import scala.concurrent.duration.Duration

import scala.slick.driver.H2Driver.simple._

object PersistentStorageService extends Logger {

  val TmpDirRemoval = new ConfigurationLocation("StorageService", "TmpDirRemoval")
  val TmpDirRegenerate = new ConfigurationLocation("StorageService", "TmpDirRegenerate")

  Workspace += (TmpDirRemoval, "P30D")
  Workspace += (TmpDirRegenerate, "P1D")

  val persistent = "persistent/"
  val tmp = "tmp/"

}

trait PersistentStorageService extends StorageService {

  import PersistentStorageService._

  case class TmpSpace(path: String, created: Long)

  @transient protected var tmpSpaceVar: Option[TmpSpace] = None
  @transient protected var persistentSpaceVar: Option[String] = None

  override def clean(implicit token: AccessToken, session: Session) = synchronized {
    ReplicaCatalog.onStorage(this).delete
    super.rmDir(baseDir)
    baseSpaceVar = None
    tmpSpaceVar = None
    persistentSpaceVar = None
  }

  override def persistentDir(implicit token: AccessToken, session: Session): String = synchronized {
    persistentSpaceVar match {
      case None ⇒
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

        persistentSpaceVar = Some(persistentPath)
        persistentPath
      case Some(s) ⇒ s
    }
  }

  override def tmpDir(implicit token: AccessToken) = synchronized {
    tmpSpaceVar match {
      case Some(tmp @ TmpSpace(path, created)) ⇒
        val create = (created + Workspace.preferenceAsDuration(TmpDirRegenerate).toMillis) < System.currentTimeMillis
        val newDir = if (create) createTmpDir else tmp
        newDir.path
      case None ⇒
        val tmpSpace = createTmpDir(token)
        tmpSpaceVar = Some(tmpSpace)
        tmpSpace.path
    }
  }

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
    TmpSpace(tmpTimePath, time)
  }

}
