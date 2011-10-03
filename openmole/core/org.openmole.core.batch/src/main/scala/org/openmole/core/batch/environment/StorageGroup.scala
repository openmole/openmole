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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.environment


import java.io.File
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.IObjectListener
import org.openmole.misc.tools.service.Priority
import org.openmole.misc.tools.service.RNG
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.StorageControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.replication.ReplicaCatalog
import collection.mutable.ArrayBuffer
import org.openmole.misc.workspace.Workspace


//object StorageGroup extends Logger

class StorageGroup(environment: BatchEnvironment, resources: Iterable[Storage]) extends Iterable[Storage] {
  
  class BatchRessourceGroupAdapterUsage extends UsageControl.IResourceReleased {
    override def ressourceReleased(obj: UsageControl) = waiting.release
  }
  
  resources.foreach {
    service =>
      val usageControl = StorageControl.usageControl(service.description)
      EventDispatcher.registerForObjectChanged(usageControl, new BatchRessourceGroupAdapterUsage, UsageControl.ResourceReleased)
  }
  

  @transient lazy val waiting = new Semaphore(0)
  @transient lazy val selectingRessource = new ReentrantLock

  override def iterator = resources.iterator
  
  def selectAService(usedFiles: Iterable[File]): (Storage, AccessToken) = {
    selectingRessource.lock
    try {
      val totalFileSize = usedFiles.map{_.size}.sum
      val onStorage = ReplicaCatalog.inCatalog(usedFiles, environment.authentication.key)

      var ret: Option[(Storage, AccessToken)] = None
      do {
        val notLoaded = resources.flatMap {   
          cur =>

          StorageControl.usageControl(cur.description).tryGetToken match {
            case None => 
             // logger.fine("no token")
              None
            case Some(token) => 
              val quality = StorageControl.qualityControl(cur.description)
              val sizeOnStorage = usedFiles.filter(onStorage.getOrElse(_, Set.empty).contains(cur.description)).map(_.size).sum
              
              val fitness = (
                quality match {
                  case Some(q) => 
                    val v = math.pow(1. * q.successRate, 2)
                    val min = Workspace.preferenceAsDouble(BatchEnvironment.MinValueForSelectionExploration)
                    if(v < min) min else v
                  case None => 1.
                }) * (if(totalFileSize != 0) (sizeOnStorage.toDouble / totalFileSize) * Workspace.preferenceAsDouble(BatchEnvironment.DataAllReadyPresentOnStoragePreference) + 1
                      else 1)
              //logger.fine("Token " + (cur, token, fitness))
              Some((cur, token, fitness))
          }
        }
             
        if (notLoaded.size > 0) {
          var selected = RNG.nextDouble * notLoaded.map{_._3}.sum
          
          for ((service, token, fitness) <- notLoaded) {    
            if(!ret.isDefined && selected <= fitness) {
             // logger.fine("resource selected "+ service)
              ret = Some((service, token))
            }
            else StorageControl.usageControl(service.description).releaseToken(token) 
            selected -= fitness
          }
        } else waiting.acquire
      } while (!ret.isDefined)
      return ret.get
    } finally selectingRessource.unlock
  }

}
