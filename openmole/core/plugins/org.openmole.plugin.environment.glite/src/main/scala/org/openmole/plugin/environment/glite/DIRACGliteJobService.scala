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

import org.openmole.core.batch.environment.SerializedJob
import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.storage.Storage
import org.openmole.misc.tools.io.FileUtil._
import fr.iscpif.gridscale.dirac.{ DIRACJobService, DIRACJobDescription }
import org.openmole.misc.tools.service.Duration._
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.core.batch.jobservice.{ BatchJobId, BatchJob }
import org.openmole.core.batch.control.LimitedAccess
import StatusFiles._
import scalax.io.Resource

trait DIRACGliteJobService extends GridScaleJobService with JobScript with LimitedAccess { js ⇒

  def environment: DIRACGliteEnvironment
  val jobService: DIRACJobService

  lazy val id = jobService.service

  protected def _submit(serializedJob: SerializedJob) = {
    import serializedJob._

    val script = Workspace.newFile("script", ".sh")
    try {
      val outputFilePath = storage.child(path, Storage.uniqName("job", ".out"))
      val _runningPath = storage.child(path, runningFile)
      val _finishedPath = storage.child(path, finishedFile)

      Resource.fromFile(script).write(generateScript(serializedJob, outputFilePath, Some(_runningPath), Some(_finishedPath)))

      val jobDescription = new DIRACJobDescription {
        def inputSandbox = Seq(script)
        def arguments = script.getName
        def executable = "/bin/bash"
        override val cpuTime = environment.cpuTime.map(_.toMinutes)
      }

      val jid = jobService.submit(jobDescription)(authentication)

      new DIRACGliteJob {
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
