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

import com.db4o.ObjectContainer
import java.io.File
import java.net.URI
import org.openmole.core.batch.control._
import org.openmole.core.batch.replication._
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace._
import fr.iscpif.gridscale.DirectoryType
import collection.JavaConversions._

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

  @transient protected var tmpSpaceVar: Option[String] = None
  @transient protected var persistentSpaceVar: Option[String] = None
  @transient private var time = System.currentTimeMillis

  override def clean(implicit token: AccessToken, objectContainer: ObjectContainer) = synchronized {
    for (r ← ReplicaCatalog.replicas(this)) ReplicaCatalog.remove(r)

    super.rmDir(baseDir)
    baseSpaceVar = None
    tmpSpaceVar = None
    persistentSpaceVar = None
    time = System.currentTimeMillis
  }

  override def persistentDir(implicit token: AccessToken, objectContainer: ObjectContainer): String = synchronized {
    persistentSpaceVar match {
      case None ⇒
        val persistentPath = child(baseDir, persistent)
        if (!super.exists(persistentPath)) super.makeDir(persistentPath)

        for (file ← super.listNames(persistentPath))
          ReplicaCatalog.rmFileIfNotUsed(this, super.child(persistentPath, file))

        persistentSpaceVar = Some(persistentPath)
        persistentPath
      case Some(s) ⇒ s
    }
  }

  override def tmpDir(implicit token: AccessToken) = synchronized {
    tmpSpaceVar match {
      case Some(space) ⇒
        if (time + Workspace.preferenceAsDuration(TmpDirRegenerate).toMilliSeconds < System.currentTimeMillis) {
          val tmpDir = createTmpDir
          tmpSpaceVar = Some(tmpDir)
          tmpDir
        }
        else space

      case None ⇒
        val tmpDir = createTmpDir(token)
        tmpSpaceVar = Some(tmpDir)
        tmpDir
    }
  }

  private def createTmpDir(implicit token: AccessToken) = {
    time = System.currentTimeMillis

    val tmpNoTime = child(baseDir, tmp)
    if (!super.exists(tmpNoTime)) super.makeDir(tmpNoTime)

    val removalDate = System.currentTimeMillis - Workspace.preferenceAsDuration(TmpDirRemoval).toMilliSeconds

    for ((name, fileType) ← super.list(tmpNoTime)) {
      val childPath = child(tmpNoTime, name)
      if (fileType == DirectoryType) {
        try {
          val timeOfDir = (if (name.endsWith("/")) name.substring(0, name.length - 1) else name).toLong
          if (timeOfDir < removalDate) backgroundRmDir(childPath)
        }
        catch {
          case (ex: NumberFormatException) ⇒ backgroundRmDir(childPath)
        }
      }
      else backgroundRmFile(childPath)
    }

    val tmpTimePath = super.child(tmpNoTime, time.toString)

    if (!super.exists(tmpTimePath)) super.makeDir(tmpTimePath)
    tmpTimePath
  }

}
