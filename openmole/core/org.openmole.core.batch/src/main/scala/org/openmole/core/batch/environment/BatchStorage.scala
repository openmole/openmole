/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.batch.environment

import java.io.FileInputStream
import java.io.IOException
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.io.FileOutputStream
import org.openmole.commons.tools.service.RNG
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchServiceControl
import org.openmole.core.batch.control.BatchStorageDescription
import org.openmole.core.batch.control.QualityControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.file.URIFileCache
import org.openmole.core.batch.file.URIFileCleaner
import org.openmole.core.batch.internal.Activator
import org.openmole.core.batch.file.IURIFile
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.workspace.ConfigurationLocation

import org.openmole.misc.workspace.IWorkspace
import scala.collection.JavaConversions._

object BatchStorage {
  val LOGGER = Logger.getLogger(BatchStorage.getClass.getName)

  val TmpDirRemoval = new ConfigurationLocation("BatchStorage", "TmpDirRemoval")
  val TmpDirRegenerate = new ConfigurationLocation("BatchStorage", "TmpDirRegenerate")
    
  Activator.getWorkspace += (TmpDirRemoval, "P30D")
  Activator.getWorkspace += (TmpDirRegenerate, "P1D")
    
  val persistent = "persistent/"
  val tmp = "tmp/"
}

class BatchStorage [ENV <: IBatchEnvironment, AUTH <: IBatchServiceAuthentication](val URI: URI, environment: ENV,  authenticationKey: IBatchServiceAuthenticationKey[AUTH], authentication: AUTH, nbAccess: Int) extends BatchService[ENV, AUTH](new BatchStorageDescription(URI), environment, authenticationKey, authentication, UsageControl(nbAccess), new QualityControl) with IBatchStorage[ENV, AUTH] {

  import BatchStorage._
  
  @transient
  var baseSpaceVar: IURIFile = null
    
  @transient
  var tmpSpaceVar: IURIFile = null
    
  @transient 
  var persistentSpaceVar: IURIFile = null
  
  @transient 
  var time = System.currentTimeMillis
 
  override def persistentSpace(token: AccessToken): IURIFile = synchronized {
    if (persistentSpaceVar == null) {
      persistentSpaceVar = baseDir(token).mkdirIfNotExist(persistent, token)
    }
    persistentSpaceVar
  }

  override def tmpSpace(token: AccessToken): IURIFile = synchronized {

    if (tmpSpaceVar == null || time + Activator.getWorkspace.preferenceAsDurationInMs(TmpDirRegenerate) < System.currentTimeMillis()) {
      time = System.currentTimeMillis

      val tmpNoTime = baseDir(token).mkdirIfNotExist(tmp, token)

      val service = Activator.getExecutorService.getExecutorService(ExecutorType.REMOVE);
      val removalDate = System.currentTimeMillis - Activator.getWorkspace.preferenceAsDurationInMs(TmpDirRemoval);

      for (dir <- tmpNoTime.list(token)) {
        val child = new URIFile(tmpNoTime, dir)
        if (child.URLRepresentsADirectory) {
          try {
            val timeOfDir = dir.substring(0, dir.length - 1).toLong

            if (timeOfDir < removalDate) {
              //LOGGER.log(Level.FINE, "Removing {0} because it's too old.", dir)
              service.submit(new URIFileCleaner(child, true, false))
            }
          } catch  {
            case (ex: NumberFormatException) =>
              //LOGGER.log(Level.FINE, "Removing {0} because it doesn't match a date.", dir)
              service.submit(new URIFileCleaner(child, true, false))
          }
        } else {
          service.submit(new URIFileCleaner(child, false, false))
        }
      }

      val tmpTmpDir = tmpNoTime.mkdirIfNotExist(time.toString(), token)
      tmpSpaceVar = tmpTmpDir
    }
    
    tmpSpaceVar
  }

  override def baseDir(token: AccessToken): IURIFile = synchronized {
    if (baseSpaceVar == null) {
      val storeFile = new URIFile(URI.toString)
      baseSpaceVar = storeFile.mkdirIfNotExist(Activator.getWorkspace.preference(IWorkspace.UniqueID) + '/', token)
    }
    baseSpaceVar
  }

  override def test: Boolean = {

    try {

      val token = BatchServiceControl.usageControl(description).waitAToken

      try {
        val lenght = 10;

        val rdm = new Array[Byte](lenght)

        RNG.nextBytes(rdm)

        val testFile = tmpSpace(token).newFileInDir("test", ".bin")
        val tmpFile = Activator.getWorkspace.newFile("test", ".bin")

        try {
          //BufferedWriter writter = new BufferedWriter(new FileWriter(tmpFile));
          val output = new FileOutputStream(tmpFile)
          try {
            output.write(rdm)
          } finally {
            output.close
          }

          URIFile.copy(tmpFile, testFile, token)
        } finally {
          tmpFile.delete
        }

        try {
          val fileCache = URIFileCache(testFile, token)
          val local = fileCache.file(false)
          val input = new FileInputStream(local)
          val resRdm = new Array[Byte](lenght)
        
          val nb = try {
            input.read(resRdm)
          } finally {
            input.close
          }
          //String tmp = read.readLine();
          if (nb == lenght && rdm.deep == resRdm.deep) {
            return true;
          }
        } finally {
          Activator.getExecutorService.getExecutorService(ExecutorType.REMOVE).submit(new URIFileCleaner(testFile, false))
        }
      } finally {
        BatchServiceControl.usageControl(description).releaseToken(token)
      }
    } catch {
      case e => LOGGER.log(Level.FINE, URI.toString, e)
    }
    return false
  }

  override def toString: String = URI.toString
}
