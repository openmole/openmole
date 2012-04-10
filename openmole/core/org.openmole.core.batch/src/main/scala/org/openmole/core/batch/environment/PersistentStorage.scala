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

object PersistentStorage extends Logger {

  val TmpDirRemoval = new ConfigurationLocation("Storage", "TmpDirRemoval")
  val TmpDirRegenerate = new ConfigurationLocation("Storage", "TmpDirRegenerate")
    
  Workspace += (TmpDirRemoval, "P30D")
  Workspace += (TmpDirRegenerate, "P1D")
    
  val persistent = "persistent/"
  val tmp = "tmp/"
  
  def createBaseDir(environment: BatchEnvironment, base: URI, dir: String, nbAccess: Int) = {
    val baseURIFile = Iterator.iterate(new File(dir))(_.getParentFile).takeWhile(_ != null).toList.reverse.filterNot(_.getName.isEmpty).foldLeft(new URIFile(base): IURIFile) {
      (uriFile, file) => uriFile.mkdirIfNotExist(file.getName)
    }
    new PersistentStorage(environment, baseURIFile.URI, nbAccess)
  }
  
}

class PersistentStorage(val environment: BatchEnvironment, URI: URI, override val nbAccess: Int) extends Storage(URI) {

  import PersistentStorage._
  
  @transient protected var tmpSpaceVar: IURIFile = null
  @transient protected var persistentSpaceVar: IURIFile = null
  @transient protected var time = System.currentTimeMillis

  override def persistentSpace(token: AccessToken): IURIFile = synchronized {
    if (persistentSpaceVar == null) {
      persistentSpaceVar = baseDir(token).mkdirIfNotExist(persistent, token)

      val inCatalog = ReplicaCatalog.inCatalog(description, environment.authentication.key)
      for (file <- persistentSpaceVar.list(token)) {
        val child = new URIFile(persistentSpaceVar, file)
        if(!inCatalog.contains(child.location)) URIFile.clean(child)
      }
        
    }
    persistentSpaceVar
  }

  override def tmpSpace(token: AccessToken): IURIFile = synchronized {

    if (tmpSpaceVar == null || time + Workspace.preferenceAsDurationInMs(TmpDirRegenerate) < System.currentTimeMillis()) {
      time = System.currentTimeMillis

      val tmpNoTime = baseDir(token).mkdirIfNotExist(tmp, token)
      val removalDate = System.currentTimeMillis - Workspace.preferenceAsDurationInMs(TmpDirRemoval);

      for (dir <- tmpNoTime.list(token)) {
        val child = new URIFile(tmpNoTime, dir)
        if (child.URLRepresentsADirectory) {
          try {
            val timeOfDir = dir.substring(0, dir.length - 1).toLong
            if (timeOfDir < removalDate) URIFile.clean(child)
          } catch  {
            case (ex: NumberFormatException) => URIFile.clean(child)
          }
        } else URIFile.clean(child)
      }

      val tmpTmpDir = tmpNoTime.mkdirIfNotExist(time.toString(), token)
      tmpSpaceVar = tmpTmpDir
    }
    
    tmpSpaceVar
  }
  
  override def baseDir(token: AccessToken): IURIFile = synchronized {
    if (baseSpaceVar == null) {
      val storeFile = new URIFile(URI.toString)
      baseSpaceVar = storeFile.mkdirIfNotExist(baseDirName, token)
    }
    baseSpaceVar
  }
  
  def baseDirName = Workspace.preference(Workspace.UniqueID) + '/'
}
