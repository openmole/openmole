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

import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.misc.tools.service.Retry._
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace

object BatchJobKiller {
  val NbRetryIfTemporaryError = new ConfigurationLocation("BatchJobKiller", "NbRetryIfTemporaryError")
  val RetryInterval = new ConfigurationLocation("BatchJobKiller", "RetryInterval")
  Workspace += (NbRetryIfTemporaryError, "3")
  Workspace += (RetryInterval, "PT10S")
}

class BatchJobKiller(job: BatchJob) extends Runnable {
  import BatchJobKiller._
  
  override def run = {
    try {
      waitAndRetryFor(job.kill, Workspace.preferenceAsInt(NbRetryIfTemporaryError), Set(classOf[TemporaryErrorException]), Workspace.preferenceAsDurationInMs(RetryInterval))
    } catch {
      case e => Logger.getLogger(classOf[BatchJobKiller].getName).log(Level.WARNING, "Could not kill job " + job.toString(), e)
    } 
  }
}
