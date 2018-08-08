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

import java.io.File
import java.util.UUID

import org.openmole.core.communication.message._
import org.openmole.core.communication.storage._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.job._
import org.openmole.plugin.environment.batch
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.signalUpload
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.file.{ uniqName, _ }
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.random._

import scala.collection.immutable.TreeSet

object UploadActor extends JavaLogger {

  def receive(msg: Upload)(implicit services: BatchEnvironment.Services) = {
    import services._

    val job = msg.job
    if (!job.state.isFinal) {
      try job.environment.serializeJob(job) match {
        case Some(sj) ⇒
          job.serializedJob = Some(sj)
          JobManager ! Uploaded(job, sj)
        case None ⇒ JobManager ! Delay(msg, BatchEnvironment.getTokenInterval)
      }
      catch {
        case e: Throwable ⇒
          JobManager ! Error(job, e, None)
          JobManager ! msg
      }
    }
  }

}
