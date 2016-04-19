/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.egi

import java.io.File
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.workspace.Workspace
import org.openmole.core.batch.storage._
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import fr.iscpif.gridscale.egi.{ WMSJobDescription }
import StatusFiles._
import org.openmole.tool.logger.Logger
import scalax.io.Resource
import org.openmole.tool.file._

object WMSJobService extends Logger

import WMSJobService._

trait WMSJobService extends GridScaleJobService { js ⇒

  val jobService: fr.iscpif.gridscale.egi.WMSJobService
  def environment: WMSEnvironment

  def jobScript =
    JobScript(
      voName = environment.voName,
      memory = environment.openMOLEMemoryValue,
      threads = environment.threadsValue,
      debug = environment.debug
    )

  val usageControl: AvailabilityQuality with JobServiceQualityControl
  import usageControl._
  def authentication = environment.authentication

  lazy val id = jobService.url.toString
  def hysteresis = Workspace.preference(EGIEnvironment.QualityHysteresis)

  override protected def _purge(j: J) = quality { super._purge(j) }

  override protected def _cancel(j: J) = quality { super._cancel(j) }

  override protected def _state(j: J) = quality { super._state(j) }

  override protected def _submit(serializedJob: SerializedJob) = quality {
    import serializedJob._

    val script = Workspace.newFile("script", ".sh")
    try {
      val outputFilePath = storage.child(path, Storage.uniqName("job", ".out"))
      val _runningPath = storage.child(path, runningFile)
      val _finishedPath = storage.child(path, finishedFile)

      val scriptContent = jobScript(serializedJob, outputFilePath)

      Resource.fromFile(script).write(scriptContent)

      val jobDescription = buildJobDescription(script)

      val jid = jobService.submit(jobDescription)
      Log.logger.fine(s"""GLite job [${jid.id}], description: \n${jobDescription.toJDL}\n with script ${scriptContent}""")

      val job = new EGIJob {
        val jobService = js
        val storage = serializedJob.storage
        val finishedPath = _finishedPath
        val runningPath = _runningPath
        val id = jid
        val resultPath = outputFilePath
      }
      if (!environment.debug) job else EGIJob.debug(job, jobDescription)
    }
    finally script.delete
  }

  protected def buildJobDescription(script: File, proxy: Option[File] = None) =
    new WMSJobDescription {
      val executable = "/bin/bash"
      val arguments = (if (environment.debug) " -x " else "") + script.getName
      val inputSandbox = List(script) ++ proxy
      override def stdOutput = if (environment.debug) "out" else ""
      override def stdError = if (environment.debug) "err" else ""
      def outputSandbox = if (environment.debug) Seq("out" → Workspace.newFile("job", ".out"), "err" → Workspace.newFile("job", ".err")) else Seq.empty

      override val memory = Some(environment.requiredMemory)
      override val cpuTime = environment.cpuTime
      override val wallTime = environment.wallTime
      override val cpuNumber = environment.cpuNumber orElse environment.threads
      override val jobType = environment.jobType
      override val smpGranularity = environment.smpGranularity orElse environment.threads
      override val retryCount = Some(0)
      override val shallowRetryCount = Some(Workspace.preference(EGIEnvironment.ShallowWMSRetryCount))
      override val myProxyServer = environment.myProxy.map(_.url)
      override val architecture = environment.architecture
      override val fuzzy = true
      override val requirements =
        environment.requirements.map(super.requirements + " && (" + _ + ")").getOrElse(super.requirements)
      override val rank = Workspace.preference(EGIEnvironment.WMSRank)
    }
}
