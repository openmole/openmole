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
import org.openmole.core.tools.io.FileUtil
import org.openmole.core.tools.service.Logger
import org.openmole.core.batch.storage.Storage
import FileUtil._
import fr.iscpif.gridscale.dirac.{ DIRACJobService ⇒ GSDIRACJobService, DIRACJobDescription ⇒ GSDIRACJobDescription }
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.core.batch.jobservice.{ BatchJobId, BatchJob }
import org.openmole.core.batch.control.{ UnlimitedAccess, LimitedAccess }
import StatusFiles._
import scalax.io.Resource
import java.io.File
import org.openmole.core.tools._

object DIRACJobService extends Logger

import DIRACJobService._

trait DIRACJobService extends GridScaleJobService with JobScript with UnlimitedAccess { js ⇒

  def environment: DIRACEnvironment
  val jobService: GSDIRACJobService

  lazy val id = jobService.service

  protected def _submit(serializedJob: SerializedJob) = {
    import serializedJob._

    val script = Workspace.newFile("script", ".sh")
    try {
      val outputFilePath = storage.child(path, Storage.uniqName("job", ".out"))
      val _runningPath = storage.child(path, runningFile)
      val _finishedPath = storage.child(path, finishedFile)

      Resource.fromFile(script).write(generateScript(serializedJob, outputFilePath, Some(_runningPath), Some(_finishedPath)))

      val jobDescription = new GSDIRACJobDescription {
        override def stdOut = if (environment.debug) Some("out") else None
        override def stdErr = if (environment.debug) Some("err") else None
        override def outputSandbox = if (environment.debug) Seq("out" -> new File("out"), "err" -> new File("err")) else Seq.empty
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
        val finishedPath = _finishedPath
        val runningPath = _runningPath
        def resultPath = outputFilePath
        def id = jid
      }
    }
    finally script.delete
  }
}
