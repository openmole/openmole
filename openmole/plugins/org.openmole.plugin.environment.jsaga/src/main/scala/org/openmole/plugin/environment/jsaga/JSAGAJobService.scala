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

package org.openmole.plugin.environment.jsaga

import java.io._
import java.net.URI
import java.util.concurrent.TimeUnit
import org.joda.time.format.ISOPeriodFormat
import org.ogf.saga.job.JobFactory
import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.{ JobService ⇒ JSJobService }
import org.ogf.saga.task._
import org.ogf.saga.error._
import org.ogf.saga.url.URLFactory
import org.openmole.core.batch.control._
import org.openmole.core.batch.authentication._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.control._
import org.openmole.core.batch.file._
import org.openmole.core.batch.authentication._
import org.openmole.misc.workspace._
import org.openmole.misc.tools.service._
import org.openmole.misc.tools.io.FileUtil._
import java.util.concurrent.ExecutionException
import scala.io.Source._

object JSAGAJobService extends Logger {
  val CreationTimeout = new ConfigurationLocation("JSAGAJobService", "CreationeTimout")

  Workspace += (CreationTimeout, "PT2M")

  def trycatch[A](t: Task[_, _], f: ⇒ A): A =
    try f
    catch {
      case (e: TimeoutException) ⇒
        t.cancel(true)
        throw e
      case (e: IOException) ⇒ throw e
      case e: ExecutionException ⇒ throw e.getCause
    }

}

trait JSAGAJobService extends JobService {

  import JSAGAJobService._

  @transient override lazy val description = new ServiceDescription(uri.toString)
  @transient lazy val jobService = createJobService(uri)

  def environment: JSAGAEnvironment
  def uri: URI

  def createJobService(uri: URI): JSJobService = {
    val t = JobFactory.createJobService(TaskMode.ASYNC, JSAGASessionService.session(uri.toString), URLFactory.createURL(uri.toString))
    trycatch(t, t.get(Workspace.preferenceAsDurationInMs(JSAGAJobService.CreationTimeout), TimeUnit.MILLISECONDS))
  }

  def newJobDescription = {
    val attributes = environment.allRequirements
    val description = JobFactory.createJobDescription

    attributes.get(CPU_TIME) match {
      case Some(value) ⇒
        val cpuTime = ISOPeriodFormat.standard.parsePeriod(value).toStandardMinutes.getMinutes
        description.setAttribute(JobDescription.WALLTIMELIMIT, cpuTime.toString)
        description.setAttribute(CPU_TIME, (cpuTime * attributes.getOrElse(CPU_COUNT, "1").toInt).toString)
      case None ⇒
    }

    attributes.filterNot(_._1 == CPU_TIME).foreach {
      case (k, v) ⇒ description.setAttribute(k, v)
    }

    description
  }

}
