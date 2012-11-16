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

package org.openmole.core.batch.storage

import org.openmole.core.batch.replication._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage._
import org.openmole.misc.updater._
import org.openmole.misc.workspace._
import scala.ref.WeakReference

class StoragesGC(storageRef: WeakReference[Iterable[StorageService]], environment: BatchEnvironment) extends IUpdatable {

  override def update: Boolean =
    storagesRef.get match {
      case Some(storages) ⇒
        ReplicaCatalog.withClient { implicit c ⇒
          for {
            storage ← storages
            replica ← ReplicaCatalog.replicas(storage)
          } {
            try
              if (!replica.sourceFile.exists || System.currentTimeMillis - replica.lastCheckExists > Workspace.preferenceAsDuration(ReplicaCatalog.NoAccessCleanTime).toMilliSeconds) {
                ReplicaCatalog.remove(replica)
                storage.backgroundRmFile(replica.path)
              }
            catch {
              case t: Throwable =>
                ReplicaCatalog.remove(replica)
                storage.backgroundRmFile(replica.path)
            }
          }
        }
        true
      case None ⇒ false
    }
}
