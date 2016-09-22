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

import org.openmole.core.replication.replicas
import org.openmole.core.updater.IUpdatable
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.batch.replication._
import org.openmole.tool.logger.Logger
import slick.driver.H2Driver.api._

import scala.ref.WeakReference

object StoragesGC extends Logger

import org.openmole.plugin.environment.batch.storage.StoragesGC.Log._

class StoragesGC(storagesRef: WeakReference[Iterable[StorageService]]) extends IUpdatable {

  override def update: Boolean =
    storagesRef.get match {
      case Some(storages) ⇒
        for {
          storage ← storages
          replica ← ReplicaCatalog.query { replicas.filter { _.storage === storage.id }.result }
        } {
          try
            if (!new File(replica.source).exists || System.currentTimeMillis - replica.lastCheckExists > Workspace.preference(ReplicaCatalog.NoAccessCleanTime).toMillis) {
              logger.fine(s"Remove gc $replica")
              ReplicaCatalog.remove(replica.id)
              storage.backgroundRmFile(replica.path)
            }
          catch {
            case t: Throwable ⇒ logger.log(FINE, "Error while garbage collecting the replicas", t)
          }
        }
        true
      case None ⇒ false
    }
}
