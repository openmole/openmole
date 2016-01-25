/*
 * Copyright (C) 10/06/13 Romain Reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
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

package org.openmole.plugin.environment.egi

import org.openmole.core.batch.environment.SerializedJob
import org.openmole.tool.file._
import org.openmole.core.batch.storage._
import fr.iscpif.gridscale.egi.{ DIRACJobService ⇒ GSDIRACJobService, DIRACJobDescription ⇒ GSDIRACJobDescription }
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.core.batch.jobservice.{ BatchJobId, BatchJob }
import org.openmole.core.batch.control.{ UnlimitedAccess, LimitedAccess }
import StatusFiles._
import org.openmole.tool.logger.Logger
import scalax.io.Resource
import java.io.File
import org.openmole.core.tools._
import fr.iscpif.gridscale.egi._

object DIRACJobService extends Logger

import DIRACJobService._

trait DIRACJobService extends GridScaleJobService { js ⇒

  def connections: Int
  def environment: DIRACEnvironment

  @transient lazy val jobService: GSDIRACJobService =
    GSDIRACJobService(
      environment.service,
      environment.group)(environment.authentication)

  @transient lazy val usageControl = new LimitedAccess(connections, Int.MaxValue)

  def jobScript =
    JobScript(
      voName = environment.voName,
      memory = environment.openMOLEMemoryValue,
      threads = environment.threadsValue,
      debug = environment.debug
    )

  @transient lazy val id = jobService.service

  protected def _submit(serializedJob: SerializedJob) = {
    import serializedJob._

    val script = Workspace.newFile("script", ".sh")
    try {
      val outputFilePath = storage.child(path, Storage.uniqName("job", ".out"))

      Resource.fromFile(script).write(jobScript(serializedJob, outputFilePath, None, None))

      val jobDescription = new GSDIRACJobDescription {
        override def stdOut = if (environment.debug) Some("out") else None
        override def stdErr = if (environment.debug) Some("err") else None
        override def outputSandbox = if (environment.debug) Seq("out" -> Workspace.newFile("job", ".out"), "err" -> Workspace.newFile("job", ".err")) else Seq.empty
        override def inputSandbox = Seq(script)
        def arguments = script.getName
        def executable = "/bin/bash"
        override val cpuTime = environment.cpuTime
      }

      val jid = jobService.submit(jobDescription)
      Log.logger.fine(s"""DIRACGLite job [${jid}], description: \n${jobDescription}""")

      new DIRACJob {
        val jobService = js
        val storage = serializedJob.storage
        def resultPath = outputFilePath
        def id = jid
      }
    }
    finally script.delete
  }
}
