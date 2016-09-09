/**
 * Created by Romain Reuillon on 08/04/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.plugin.environment.egi

import java.net.URI

import fr.iscpif.gridscale.tools.findWorking
import fr.iscpif.gridscale.egi.BDII
import org.openmole.plugin.environment.batch.control.LimitedAccess
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchExecutionJob, MemoryRequirement }
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.updater.Updater
import org.openmole.core.workflow.job.Job
import org.openmole.core.workflow.dsl._
import org.openmole.core.workspace.{ Decrypt, Workspace }

import scala.concurrent.duration.Duration
import scala.ref.WeakReference
import org.openmole.core.tools.math._
import org.openmole.tool.cache.Cache

object WMSEnvironment {

  def apply(
    voName:         String,
    bdii:           OptionalArgument[String]      = None,
    vomsURLs:       OptionalArgument[Seq[String]] = None,
    fqan:           OptionalArgument[String]      = None,
    openMOLEMemory: OptionalArgument[Int]         = None,
    memory:         OptionalArgument[Int]         = None,
    cpuTime:        OptionalArgument[Duration]    = None,
    wallTime:       OptionalArgument[Duration]    = None,
    cpuNumber:      OptionalArgument[Int]         = None,
    jobType:        OptionalArgument[String]      = None,
    smpGranularity: OptionalArgument[Int]         = None,
    myProxy:        OptionalArgument[MyProxy]     = None,
    architecture:   OptionalArgument[String]      = None,
    threads:        OptionalArgument[Int]         = None,
    requirements:   OptionalArgument[String]      = None,
    debug:          Boolean                       = false,
    name:           OptionalArgument[String]      = None
  )(implicit authentication: EGIAuthentication, uncypher: Decrypt) =
    new WMSEnvironment(
      voName = voName,
      bdiis = bdii.map(s ⇒ Seq(EGIEnvironment.toBDII(new URI(s)))).getOrElse(EGIEnvironment.defaultBDIIs),
      vomsURLs = vomsURLs.getOrElse(EGIAuthentication.getVMOSOrError(voName)),
      fqan = fqan,
      openMOLEMemory = openMOLEMemory,
      memory = memory,
      cpuTime = cpuTime,
      wallTime = wallTime,
      cpuNumber = cpuNumber,
      jobType = jobType,
      smpGranularity = smpGranularity,
      myProxy = myProxy,
      architecture = architecture,
      threads = threads,
      requirements = requirements,
      debug = debug,
      name = name
    )(authentication, uncypher)
}

class EGIBatchExecutionJob(val job: Job, val environment: WMSEnvironment) extends BatchExecutionJob {
  def trySelectStorage() = environment.trySelectAStorage(usedFileHashes)
  def trySelectJobService() = environment.trySelectAJobService
}

class WMSEnvironment(
    val voName:                  String,
    val bdiis:                   Seq[BDII],
    val vomsURLs:                Seq[String],
    val fqan:                    Option[String],
    override val openMOLEMemory: Option[Int],
    val memory:                  Option[Int],
    val cpuTime:                 Option[Duration],
    val wallTime:                Option[Duration],
    val cpuNumber:               Option[Int],
    val jobType:                 Option[String],
    val smpGranularity:          Option[Int],
    val myProxy:                 Option[MyProxy],
    val architecture:            Option[String],
    override val threads:        Option[Int],
    val requirements:            Option[String],
    val debug:                   Boolean,
    override val name:           Option[String]
)(implicit a: EGIAuthentication, decrypt: Decrypt) extends BatchEnvironment with MemoryRequirement with BDIIStorageServers with EGIEnvironmentId { env ⇒

  import EGIEnvironment._

  def connectionsByWMS = Workspace.preference(ConnectionsByWMS)

  type JS = WMSJobService

  val registerAgents = Cache {
    Updater.delay(new EagerSubmissionAgent(WeakReference(this), EGIEnvironment.EagerSubmissionThreshold))
    None
  }

  def executionJob(job: Job) = new EGIBatchExecutionJob(job, this)

  override def submit(job: Job) = {
    registerAgents()
    super.submit(job)
  }

  def proxyCreator = authentication

  @transient lazy val authentication =
    EGIAuthentication.initialise(a)(
      vomsURLs,
      voName,
      fqan
    )(decrypt)

  def jobServices = _jobServices()
  val _jobServices = Cache {
    val bdiiWMS = findWorking(bdiis, (b: BDII) ⇒ b.queryWMSLocations(voName))
    bdiiWMS.map {
      js ⇒
        new WMSJobService {
          val usageControl = new AvailabilityQuality with JobServiceQualityControl {
            override val usageControl = new LimitedAccess(connectionsByWMS, Int.MaxValue)
            override val hysteresis = Workspace.preference(EGIEnvironment.QualityHysteresis)
          }

          val jobService = fr.iscpif.gridscale.egi.WMSJobService(js, connectionsByWMS, proxyRenewalTime)(authentication)
          def environment = env
        }
    }
  }

  def trySelectAJobService = {
    val jss = jobServices
    if (jss.isEmpty) throw new InternalProcessingError("No job service available for the environment.")

    val nonEmpty = jss.filter(!_.usageControl.isEmpty)
    def jobFactor(j: WMSJobService) = (j.usageControl.running.toDouble / j.usageControl.submitted) * (j.usageControl.totalDone.toDouble / j.usageControl.totalSubmitted)

    lazy val times = nonEmpty.map(_.usageControl.time)
    lazy val maxTime = times.max
    lazy val minTime = times.min

    lazy val availablities = nonEmpty.map(_.usageControl.availability)
    lazy val maxAvailability = availablities.max
    lazy val minAvailability = availablities.min

    lazy val jobFactors = nonEmpty.map(jobFactor)
    lazy val maxJobFactor = jobFactors.max
    lazy val minJobFactor = jobFactors.min

    def rate(js: WMSJobService) = {
      val time = js.usageControl.time
      val timeFactor = if (minTime == maxTime) 1.0 else 1.0 - time.normalize(minTime, maxTime)

      val availability = js.usageControl.availability
      val availabilityFactor = if (minAvailability == maxAvailability) 1.0 else 1.0 - availability.normalize(minTime, maxTime)

      val jobFactor =
        if (js.usageControl.submitted > 0 && js.usageControl.totalSubmitted > 0) ((js.usageControl.running.toDouble / js.usageControl.submitted) * (js.usageControl.totalDone / js.usageControl.totalSubmitted)).normalize(minJobFactor, maxJobFactor)
        else 0.0

      math.pow(
        Workspace.preference(JobServiceJobFactor) * jobFactor +
          Workspace.preference(JobServiceTimeFactor) * timeFactor +
          Workspace.preference(JobServiceAvailabilityFactor) * availability +
          Workspace.preference(JobServiceSuccessRateFactor) * js.usageControl.successRate,
        Workspace.preference(JobServiceFitnessPower)
      )
    }

    select(jss.toList, rate)
  }

  override def runtimeSettings = super.runtimeSettings.copy(archiveResult = true)
}
