/*
 * Copyright (C) 2012 Romain Reuillon
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

import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, UsageControl }
import org.openmole.tool.logger.JavaLogger
import org.openmole.core.tools.service.Retry._

object KillerActor extends JavaLogger {

  def receive(msg: KillBatchJob)(implicit services: BatchEnvironment.Services) = {
    import services._

    val KillBatchJob(bj) = msg
    try UsageControl.tryWithToken(bj.usageControl) {
      case Some(t) ⇒ JobManager.killBatchJob(bj, t)
      case None ⇒
        JobManager ! Delay(msg, BatchEnvironment.getTokenInterval)
    } catch {
      case e: Throwable ⇒ Log.logger.log(Log.FINE, "Could not kill job.", e)
    }
  }

}
