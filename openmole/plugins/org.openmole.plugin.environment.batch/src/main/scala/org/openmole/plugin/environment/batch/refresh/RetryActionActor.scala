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

import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment }
import org.openmole.tool.logger.{ JavaLogger, LoggerService }

object RetryActionActor {

  def receive(msg: RetryAction)(implicit services: BatchEnvironment.Services) = {
    import services._

    val RetryAction(action) = msg
    try {
      val retry = action()
      if (retry) JobManager ! Delay(msg, BatchEnvironment.getTokenInterval)
    }
    catch {
      case t: Throwable =>
        LoggerService.fine("Error when deleting a file", t)
    }
  }

}