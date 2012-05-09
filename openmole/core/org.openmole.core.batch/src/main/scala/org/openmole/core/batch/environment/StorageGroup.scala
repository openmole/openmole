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
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.tools.service.Random
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.StorageControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.misc.workspace.Workspace
import scala.annotation.tailrec
import ServiceGroup._

object StorageGroup extends Logger

import StorageGroup._

class StorageGroup(environment: BatchEnvironment, resources: Iterable[Storage]) extends ServiceGroup with Iterable[Storage] {

  class BatchRessourceGroupAdapterUsage extends EventListener[UsageControl] {
    override def triggered(subMole: UsageControl, ev: Event[UsageControl]) = waiting.release
  }

  resources.foreach {
    service ⇒
      val usageControl = UsageControl.get(service.description)
      EventDispatcher.listen(usageControl, new BatchRessourceGroupAdapterUsage, classOf[UsageControl.ResourceReleased])
  }

  @transient lazy val waiting = new Semaphore(0)
  @transient lazy val selectingRessource = new ReentrantLock

  override def iterator = resources.iterator

  def selectAService(usedFiles: Iterable[File]): (Storage, AccessToken) = {
    if (resources.size == 1) {
      val r = resources.head
      return (r, UsageControl.get(r.description).waitAToken)
    }

    selectingRessource.lock
    try {
      val totalFileSize = usedFiles.map { _.size }.sum
      val onStorage = ReplicaCatalog.inCatalog(usedFiles, environment.authentication.key)

      def fitness = {
        resources.flatMap {
          cur ⇒

            UsageControl.get(cur.description).tryGetToken match {
              case None ⇒ None
              case Some(token) ⇒
                val sizeOnStorage = usedFiles.filter(onStorage.getOrElse(_, Set.empty).contains(cur.description)).map(_.size).sum

                val fitness = orMin(
                  StorageControl.qualityControl(cur.description) match {
                    case Some(q) ⇒ math.pow(q.successRate, 3)
                    case None ⇒ 1.
                  }) * (if (totalFileSize != 0) (sizeOnStorage.toDouble / totalFileSize) else 1)
                Some((cur, token, fitness))
            }
        }
      }.toList

      
      
      @tailrec def selected(value: Double, storages: List[(Storage, AccessToken, Double)]): Option[(Storage, AccessToken)] =
        storages.headOption match {
          case Some((storage, token, fitness)) ⇒
            if (value <= fitness) {
              releaseAll(storages.tail)
              Some((storage, token))
            } else {
              release(storage, token)
              selected(value - fitness, storages.tail)
            }
          case None ⇒ None
        }

      @tailrec def wait: (Storage, AccessToken) = {
        val notLoaded = fitness
        logger.finest(notLoaded.mkString(", "))
        
        selected(Random.default.nextDouble * notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum, notLoaded) match {
          case Some(storage) ⇒ storage
          case None ⇒
            waiting.acquire
            wait
        }
      }

      wait
    } finally selectingRessource.unlock
  }

  private def releaseAll(storages: List[(Storage, AccessToken, Double)]) =
    storages.foreach {
      case (storage, token, _) ⇒ release(storage, token)
    }

  private def release(storage: Storage, token: AccessToken) =
    UsageControl.get(storage.description).releaseToken(token)

}
