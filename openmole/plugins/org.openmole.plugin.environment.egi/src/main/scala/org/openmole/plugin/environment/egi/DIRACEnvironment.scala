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

import java.net.URI

import fr.iscpif.gridscale.authentication.P12Authentication
import org.openmole.core.batch.environment.{ BatchExecutionJob, BatchEnvironment }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice._
import org.openmole.core.workflow.dsl._
import org.openmole.core.updater.Updater
import org.openmole.core.workflow.execution.ExecutionJob
import org.openmole.core.workflow.job.Job
import org.openmole.core.workspace._
import fr.iscpif.gridscale.egi._
import fr.iscpif.gridscale.egi.{ DIRACJobService ⇒ GSDIRACJobService }
import concurrent.duration._
import scala.ref.WeakReference

object DIRACEnvironment {

  val Connections = ConfigurationLocation("DIRACEnvironment", "Connections", Some(100))
  val EagerSubmissionThreshold = ConfigurationLocation("DIRACEnvironment", "EagerSubmissionThreshold", Some(0.2))

  Workspace setDefault Connections
  Workspace setDefault EagerSubmissionThreshold

  def apply(
    voName:         String,
    service:        OptionalArgument[String]      = None,
    group:          OptionalArgument[String]      = None,
    bdii:           OptionalArgument[String]      = None,
    vomsURLs:       OptionalArgument[Seq[String]] = None,
    setup:          OptionalArgument[String]      = None,
    fqan:           OptionalArgument[String]      = None,
    cpuTime:        OptionalArgument[Duration]    = None,
    openMOLEMemory: OptionalArgument[Int]         = None,
    debug:          Boolean                       = false,
    name:           OptionalArgument[String]      = None
  )(implicit authentication: EGIAuthentication, decrypt: Decrypt) =
    new DIRACEnvironment(
      voName = voName,
      service = service,
      group = group,
      bdii = bdii.map(new URI(_)).getOrElse(new URI(Workspace.preference(EGIEnvironment.DefaultBDII))),
      vomsURLs = vomsURLs.getOrElse(EGIAuthentication.getVMOSOrError(voName)),
      setup = setup.getOrElse("Dirac-Production"),
      fqan = fqan,
      cpuTime = cpuTime,
      openMOLEMemory = openMOLEMemory,
      debug = debug,
      name = name
    )(authentication, decrypt)

}

class DiracBatchExecutionJob(val job: Job, val environment: DIRACEnvironment) extends BatchExecutionJob {

  def trySelectStorage() = environment.trySelectAStorage(usedFileHashes)

  def trySelectJobService() = {
    val js = environment.jobService
    js.tryGetToken.map(js → _)
  }

}

class DIRACEnvironment(
    val voName:                  String,
    val service:                 Option[String],
    val group:                   Option[String],
    val bdii:                    URI,
    val vomsURLs:                Seq[String],
    val setup:                   String,
    val fqan:                    Option[String],
    val cpuTime:                 Option[Duration],
    override val openMOLEMemory: Option[Int],
    val debug:                   Boolean,
    override val name:           Option[String]
)(implicit a: EGIAuthentication, decrypt: Decrypt) extends BatchEnvironment with BDIIStorageServers with EGIEnvironmentId { env ⇒

  type JS = DIRACJobService

  @transient lazy val registerAgents = {
    Updater.delay(new EagerSubmissionAgent(WeakReference(this), DIRACEnvironment.EagerSubmissionThreshold))
    None
  }

  override def submit(job: Job) = {
    registerAgents
    super.submit(job)
  }

  def bdiiServer: BDII = BDII(bdii.getHost, bdii.getPort, Workspace.preference(EGIEnvironment.FetchResourcesTimeOut))

  def executionJob(job: Job) = new DiracBatchExecutionJob(job, this)

  @transient lazy val authentication: P12Authentication = DIRACAuthentication.initialise(a)(decrypt)

  @transient lazy val proxyCreator = {
    EGIAuthentication.initialise(a)(
      vomsURLs,
      voName,
      fqan
    )(decrypt)
  }

  @transient lazy val jobService = new DIRACJobService {
    val connections = Workspace.preference(DIRACEnvironment.Connections)
    val environment = env
  }

  override def runtimeSettings = super.runtimeSettings.copy(archiveResult = true)
}
