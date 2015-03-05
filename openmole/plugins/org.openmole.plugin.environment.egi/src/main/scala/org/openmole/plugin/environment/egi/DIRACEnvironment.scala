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

import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.filedeleter.FileDeleter
import org.openmole.core.updater.Updater
import org.openmole.core.workflow.job.Job
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation, AuthenticationProvider }
import fr.iscpif.gridscale.glite.BDII
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
    debug: Boolean = false)(implicit authentications: AuthenticationProvider) =
    new DIRACEnvironment(
      voName,
      service,
      group.getOrElse(voName + "_user"),
      bdii.getOrElse(Workspace.preference(EGIEnvironment.DefaultBDII)),
      vomsURL.getOrElse(EGIAuthentication.getVMOSOrError(voName)),
      setup.getOrElse("Dirac-Production"),
      fqan,
      cpuTime,
      openMOLEMemory,
      debug
    )(authentications)

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
    val debug: Boolean)(implicit authentications: AuthenticationProvider) extends BatchEnvironment with BDIISRMServers with EGIEnvironmentId with LCGCp { env ⇒

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

  def getAuthentication = authentications(classOf[DIRACAuthentication]).headOption.getOrElse(throw new UserBadDataError("No authentication found for DIRAC"))

  @transient lazy val authentication = DIRACAuthentication.initialise(getAuthentication)(authentications)

  @transient lazy val proxyCreator = {
    EGIAuthentication.initialise(getAuthentication)(
      vomsURL,
      voName,
      EGIEnvironment.proxyTime,
      fqan)(authentications).cache(EGIEnvironment.proxyRenewalDelay)
  }

  def allJobServices = List(jobService)

  @transient lazy val jobService = new DIRACJobService {
    val environment = env
    val jobService = new GSDIRACJobService {
      def group = env.group
      def service = env.service
      def credential = env.authentication
      override def maxConnections = Workspace.preferenceAsInt(DIRACEnvironment.LocalThreads)
    }
  }

}
