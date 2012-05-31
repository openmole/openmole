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

package org.openmole.plugin.environment.jsaga

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import org.ogf.saga.job.Job
import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.ogf.saga.task.TaskMode
import org.ogf.saga.url.URLFactory
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.authentication._
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.batch.environment.Runtime
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.batch.jsaga.JSAGASessionService
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.tools.io.FileUtil._
import scala.io.Source._

object JSAGAJobService extends Logger {
  val CreationTimeout = new ConfigurationLocation("JSAGAJobService", "CreationeTimout")

  Workspace += (CreationTimeout, "PT2M")
}

trait JSAGAJobService extends JobService {

  import JSAGAJobService._

  @transient override lazy val description = new ServiceDescription(uri.toString)

  @transient lazy val jobService = {
    val task = {
      val url = URLFactory.createURL(uri.toString)
      JobFactory.createJobService(TaskMode.ASYNC, JSAGASessionService.session(uri.toString), url)
    }

    task.get(Workspace.preferenceAsDurationInMs(JSAGAJobService.CreationTimeout), TimeUnit.MILLISECONDS)
  }

  def uri: URI

}
