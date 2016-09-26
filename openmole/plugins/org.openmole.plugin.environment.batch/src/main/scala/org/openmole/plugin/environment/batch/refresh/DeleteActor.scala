/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment.batch.refresh

import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.tool.logger.Logger

object DeleteActor extends Logger

import org.openmole.plugin.environment.batch.refresh.DeleteActor.Log._

class DeleteActor(jobManager: JobManager) {
  def receive(msg: DeleteFile) = {
    val DeleteFile(storage, path, directory) = msg
    try storage.tryWithToken {
      case Some(t) ⇒
        if (directory) storage.rmDir(path)(t) else storage.rmFile(path)(t)
      case None ⇒
        jobManager ! Delay(msg, BatchEnvironment.getTokenInterval)
    }
    catch {
      case t: Throwable ⇒
        logger.log(FINE, "Error when deleting a file", t)
    }
  }
}