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

package org.openmole.plugin.environment.glite

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URI
import java.util.UUID
import org.openmole.core.batch.control._
import org.openmole.core.batch.storage.{ StorageService, Storage }
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.batch.environment.Runtime
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.control.AccessToken
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import fr.iscpif.gridscale.jobservice.{ WMSJobService, WMSJobDescription }
import scala.collection.JavaConversions._
import scala.io.Source
import org.openmole.misc.tools.service.Duration._
import StatusFiles._

trait GliteJobService extends GridScaleJobService with JobServiceQualityControl with LimitedAccess with AvailabitityQuality with JobScript { js ⇒

  val jobService: WMSJobService
  def environment: GliteEnvironment

  def authentication = environment.authentication

  lazy val id = jobService.url.toString
  def hysteresis = Workspace.preferenceAsInt(GliteEnvironment.QualityHysteresis)

  var delegated = false

  def delegated[T](f: ⇒ T): T = synchronized {
    if (!delegated) {
      jobService.delegate(authentication)
      delegated = true
    }
    f
  }

  override protected def _purge(j: J) = quality { delegated { super._purge(j) } }

  override protected def _cancel(j: J) = quality { delegated { super._cancel(j) } }

  override protected def _state(j: J) = quality { delegated { super._state(j) } }

  override protected def _submit(serializedJob: SerializedJob) = quality {
    delegated {
      import serializedJob._

      val script = Workspace.newFile("script", ".sh")
      try {
        val outputFilePath = storage.child(path, Storage.uniqName("job", ".out"))
        val _runningPath = storage.child(path, runningFile)
        val _finishedPath = storage.child(path, finishedFile)

        val os = script.bufferedOutputStream
        try generateScript(serializedJob, outputFilePath, Some(_runningPath), Some(_finishedPath), os)
        finally os.close

        //logger.fine(ISource.fromFile(script).mkString)

        val jobDescription = buildJobDescription(script)

        //logger.fine(jobDescription.toJDL)

        val jid = jobService.submit(jobDescription)(authentication)

        new GliteJob {
          val jobService = js
          val storage = serializedJob.storage
          val finishedPath = _finishedPath
          val runningPath = _runningPath
          val id = jid
          val resultPath = outputFilePath
        }
      }
      finally script.delete
    }
  }

  protected def buildJobDescription(script: File) =
    new WMSJobDescription {
      val executable = "/bin/bash"
      val arguments = script.getName
      val inputSandbox = List(script)
      val outputSandbox = List.empty
      override val memory = Some(environment.requieredMemory)
      override val cpuTime = environment.cpuTime.map(_.toMinutes)
      override val wallTime = environment.wallTime.map(_.toMinutes)
      override val cpuNumber = environment.cpuNumber orElse environment.threads
      override val jobType = environment.jobType
      override val smpGranularity = environment.smpGranularity orElse environment.threads
      override val retryCount = Some(0)
      override val shallowRetryCount = Some(Workspace.preferenceAsInt(GliteEnvironment.ShallowWMSRetryCount))
      override val myProxyServer = environment.myProxy.map(_.url)
      override val architecture = environment.architecture
      override val fuzzy = true
    }

}
