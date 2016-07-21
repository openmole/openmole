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
import org.openmole.core.batch.storage._
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.core.batch.jobservice.{ BatchJob, BatchJobId }
import org.openmole.core.batch.control.{ UnlimitedAccess }
import org.openmole.tool.logger.Logger
import fr.iscpif.gridscale.{ egi ⇒ gridscale }
import fr.iscpif.gridscale.egi._
import scalax.io.Resource

object DIRACJobService extends Logger

import DIRACJobService._

trait DIRACJobService extends GridScaleJobService { js ⇒

  def getToken = jobService.jobService.token

  def environment: DIRACEnvironment
  def usageControl = UnlimitedAccess

  def diracService = {
    lazy val gridscale.DIRACJobService.Service(service, group) = gridscale.DIRACJobService.getService(environment.voName)

    val serviceValue = environment.service.getOrElse(service)
    val groupValue = environment.group.getOrElse(group)

    gridscale.DIRACJobService.Service(serviceValue, groupValue)
  }

  val jobService = {
    val js =
      gridscale.DIRACGroupedJobService(
        environment.voName,
        service = Some(diracService),
        statusQueryInterval = environment.updateInterval.minUpdateInterval,
        jobsByGroup = Workspace.preference(DIRACEnvironment.JobsByGroup)
      )(environment.authentication)

    js.delegate(environment.authentication.certificate, environment.authentication.password)
    js
  }

  def jobScript =
    JobScript(
      voName = environment.voName,
      memory = environment.openMOLEMemoryValue,
      threads = environment.threadsValue,
      debug = environment.debug
    )

  def id = diracService

  protected def _submit(serializedJob: SerializedJob) = {
    import serializedJob._

    val script = Workspace.newFile("script", ".sh")
    try {
      val outputFilePath = storage.child(path, Storage.uniqName("job", ".out"))

      Resource.fromFile(script).write(jobScript(serializedJob, outputFilePath, None, None))

      val jobDescription = gridscale.DIRACJobDescription(
        stdOut = if (environment.debug) Some("out") else None,
        stdErr = if (environment.debug) Some("err") else None,
        outputSandbox = if (environment.debug) Seq("out" → Workspace.newFile("job", ".out"), "err" → Workspace.newFile("job", ".err")) else Seq.empty,
        inputSandbox = Seq(script),
        arguments = script.getName,
        executable = "/bin/bash",
        cpuTime = environment.cpuTime
      )

      val jid = jobService.submit(jobDescription)
      Log.logger.fine(s"""DIRAC job [${jid}], description: \n${jobDescription}""")

      new DIRACJob {
        val jobService = js
        val storage = serializedJob.storage
        def resultPath = outputFilePath
        def id = jid
      }
    }
    finally script.delete
  }

  override protected def _delete(j: J) = if (!environment.debug) super._delete(j)
}
