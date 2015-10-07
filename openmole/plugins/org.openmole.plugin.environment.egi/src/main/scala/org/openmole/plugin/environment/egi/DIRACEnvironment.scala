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

package org.openmole.plugin.environment.egi

import org.openmole.core.batch.environment.{ BatchExecutionJob, BatchEnvironment }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.filedeleter.FileDeleter
import org.openmole.core.fileservice.FileService
import org.openmole.core.updater.Updater
import org.openmole.core.workflow.execution.ExecutionJob
import org.openmole.core.workflow.job.Job
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation, AuthenticationProvider }
import fr.iscpif.gridscale.egi.BDII
import fr.iscpif.gridscale.dirac.{ DIRACJobService ⇒ GSDIRACJobService }
import concurrent.duration._
import scala.ref.WeakReference

object DIRACEnvironment {

  val LocalThreads = new ConfigurationLocation("DIRACEnvironment", "LocalThreads")
  val EagerSubmissionThreshold = ConfigurationLocation("DIRACEnvironment", "EagerSubmissionThreshold")

  Workspace += (LocalThreads, "100")
  Workspace += (EagerSubmissionThreshold, "0.2")

  def apply(
    voName: String,
    service: String,
    group: Option[String] = None,
    bdii: Option[String] = None,
    vomsURL: Option[String] = None,
    setup: Option[String] = None,
    fqan: Option[String] = None,
    cpuTime: Option[Duration] = None,
    openMOLEMemory: Option[Int] = None,
    debug: Boolean = false,
    name: Option[String] = None)(implicit authentications: AuthenticationProvider) =
    new DIRACEnvironment(
      voName = voName,
      service = service,
      group = group.getOrElse(voName + "_user"),
      bdii = bdii.getOrElse(Workspace.preference(EGIEnvironment.DefaultBDII)),
      vomsURL = vomsURL.getOrElse(EGIAuthentication.getVMOSOrError(voName)),
      setup = setup.getOrElse("Dirac-Production"),
      fqan = fqan,
      cpuTime = cpuTime,
      openMOLEMemory = openMOLEMemory,
      debug = debug,
      name = name
    )(authentications)

}

class DiracBatchExecutionJob(val job: Job, val environment: DIRACEnvironment) extends BatchExecutionJob {

  def selectStorage() = environment.selectAStorage(usedFileHashes)

  def selectJobService() = {
    val js = environment.jobService
    (js, js.waitAToken)
  }

}

class DIRACEnvironment(
    val voName: String,
    val service: String,
    val group: String,
    val bdii: String,
    val vomsURL: String,
    val setup: String,
    val fqan: Option[String],
    val cpuTime: Option[Duration],
    override val openMOLEMemory: Option[Int],
    val debug: Boolean,
    override val name: Option[String])(implicit authentications: AuthenticationProvider) extends BatchEnvironment with BDIISRMServers with EGIEnvironmentId with LCGCp { env ⇒

  type JS = DIRACJobService

  @transient lazy val registerAgents = {
    Updater.delay(new EagerSubmissionAgent(WeakReference(this), DIRACEnvironment.EagerSubmissionThreshold))
    None
  }

  override def submit(job: Job) = {
    registerAgents
    super.submit(job)
  }

  def bdiiServer: BDII = new BDII(bdii)

  def executionJob(job: Job) = new DiracBatchExecutionJob(job, this)

  def getAuthentication = authentications(classOf[EGIAuthentication]).headOption.getOrElse(throw new UserBadDataError("No authentication found for DIRAC"))

  @transient lazy val authentication = DIRACAuthentication.initialise(getAuthentication)(authentications)

  @transient lazy val proxyCreator = {
    EGIAuthentication.initialise(getAuthentication)(
      vomsURL,
      voName,
      EGIEnvironment.proxyTime,
      fqan)(authentications).cache(EGIEnvironment.proxyRenewalDelay)
  }

  @transient lazy val jobService = new DIRACJobService {
    val environment = env
    val jobService = new GSDIRACJobService {
      def group = env.group
      def service = env.service
      def credential = env.authentication
      override def maxConnections = Workspace.preferenceAsInt(DIRACEnvironment.LocalThreads)
    }
  }

  override def runtimeSettings = super.runtimeSettings.copy(archiveResult = true)
}
