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

package org.openmole.core.batch.environment

import java.io.File
import java.net.URI
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import collection.JavaConversions._

object PersistentStorage extends Logger {

  val TmpDirRemoval = new ConfigurationLocation("Storage", "TmpDirRemoval")
  val TmpDirRegenerate = new ConfigurationLocation("Storage", "TmpDirRegenerate")

  Workspace += (TmpDirRemoval, "P30D")
  Workspace += (TmpDirRegenerate, "P1D")

  val persistent = "persistent/"
  val tmp = "tmp/"

  def createBaseDir(environment: BatchEnvironment, base: URI, dir: String, nbAccess: Int) = {
    val baseURIFile =
      Iterator.iterate(new File(dir))(_.getParentFile).takeWhile(_ != null).toList.reverse.filterNot(_.getName.isEmpty).foldLeft(new URIFile(base): IURIFile) {
        (uriFile, file) ⇒
          uriFile.mkdirIfNotExist(file.getName)
      }

    new PersistentStorage(environment, baseURIFile.URI, nbAccess)
  }

}

class PersistentStorage(
    val environment: BatchEnvironment,
    val URI: URI,
    override val nbAccess: Int) extends Storage {

  import PersistentStorage._

  @transient protected var baseSpaceVar: Option[IURIFile] = None
  @transient protected var tmpSpaceVar: Option[IURIFile] = None
  @transient protected var persistentSpaceVar: Option[IURIFile] = None
  @transient protected var time = System.currentTimeMillis

  override def clean(token: AccessToken) = synchronized {
    for (r ← ReplicaCatalog.getReplica(description, environment.authentication.key)) ReplicaCatalog.remove(r)

    baseDir(token).remove(token)
    baseSpaceVar = None
    tmpSpaceVar = None
    persistentSpaceVar = None
    time = System.currentTimeMillis
  }

  override def persistentSpace(token: AccessToken): IURIFile = synchronized {
    persistentSpaceVar match {
      case None ⇒
        val persistentSpace = baseDir(token).mkdirIfNotExist(persistent, token)
        val inCatalog = ReplicaCatalog.inCatalog(description, environment.authentication.key)
        for (file ← persistentSpace.list(token)) {
          val child = new URIFile(persistentSpace, file)
          if (!inCatalog.contains(child.location)) URIFile.clean(child)
        }

        persistentSpaceVar = Some(persistentSpace)
        persistentSpace
      case Some(s) ⇒ s
    }
  }

  override def tmpSpace(token: AccessToken): IURIFile = synchronized {
    tmpSpaceVar match {
      case Some(space) ⇒
        if (time + Workspace.preferenceAsDurationInMs(TmpDirRegenerate) < System.currentTimeMillis) {
          val tmpDir = createTmpDir(token)
          tmpSpaceVar = Some(tmpDir)
          tmpDir
        } else space

      case None ⇒
        val tmpDir = createTmpDir(token)
        tmpSpaceVar = Some(tmpDir)
        tmpDir
    }
  }

  private def createTmpDir(token: AccessToken) = {
    time = System.currentTimeMillis

    val tmpNoTime = baseDir(token).mkdirIfNotExist(tmp, token)
    val removalDate = System.currentTimeMillis - Workspace.preferenceAsDurationInMs(TmpDirRemoval);

    for (dir ← tmpNoTime.list(token)) {
      val child = new URIFile(tmpNoTime, dir)
      if (child.URLRepresentsADirectory) {
        try {
          val timeOfDir = dir.substring(0, dir.length - 1).toLong
          if (timeOfDir < removalDate) URIFile.clean(child)
        } catch {
          case (ex: NumberFormatException) ⇒ URIFile.clean(child)
        }
      } else URIFile.clean(child)
    }

    tmpNoTime.mkdirIfNotExist(time.toString, token)
  }

  override def baseDir(token: AccessToken): IURIFile = synchronized {
    baseSpaceVar match {
      case Some(s) ⇒ s
      case None ⇒
        val storeFile = new URIFile(URI.toString)
        val baseSpace = storeFile.mkdirIfNotExist(baseDirName, token)
        baseSpaceVar = Some(baseSpace)
        baseSpace
    }
  }

  def baseDirName = Workspace.preference(Workspace.UniqueID) + '/'
}
