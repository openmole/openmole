/*
 * Copyright (C) 10/06/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.glite

import org.openmole.core.batch.storage.StorageService
import org.openmole.core.model.execution.ExecutionState._

object StatusFiles {
  val finishedFile = "finished"
  val runningFile = "running"
}

trait StatusFiles {

  val storage: StorageService
  val finishedPath: String
  val runningPath: String

  def testStatusFile(state: ExecutionState) =
    state match {
      case SUBMITTED ⇒
        storage.tryWithToken {
          case Some(t) ⇒
            if (storage.exists(runningPath)(t)) RUNNING
            else state
          case None ⇒ state
        }
      case RUNNING ⇒
        storage.tryWithToken {
          case Some(t) ⇒
            if (storage.exists(finishedPath)(t)) DONE
            else state
          case None ⇒ state
        }
      case _ ⇒ state
    }
}
