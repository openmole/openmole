/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.environment.batch.storage

import java.io.File

import org.openmole.core.db._
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.threadprovider.IUpdatable
import org.openmole.tool.logger.JavaLogger
import slick.driver.H2Driver.api._

import scala.ref.WeakReference

object StoragesGC extends JavaLogger

import org.openmole.plugin.environment.batch.storage.StoragesGC.Log._

class StoragesGC(storageRef: WeakReference[StorageService[_]]) extends IUpdatable {

  override def update: Boolean =
    storageRef.get match {
      case Some(storage) ⇒
        import storage.environment.services

        for {
          replica ← services.replicaCatalog.query { replicas.filter { _.storage === storage.id }.result }
        } {
          try
            if (!new File(replica.source).exists || System.currentTimeMillis - replica.lastCheckExists > services.preference(ReplicaCatalog.NoAccessCleanTime).millis) {
              logger.fine(s"Remove gc $replica")
              services.replicaCatalog.remove(replica.id)
              StorageService.backgroundRmFile(storage, replica.path)
            }
          catch {
            case t: Throwable ⇒ logger.log(FINE, "Error while garbage collecting the replicas", t)
          }
        }
        true
      case None ⇒ false
    }
}
