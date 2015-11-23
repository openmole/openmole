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

package org.openmole.plugin.environment.egi

import java.util.concurrent.TimeUnit

import org.eclipse.osgi.service.environment.EnvironmentInfo
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.filedeleter.FileDeleter
import org.openmole.core.fileservice.FileService
import org.openmole.tool.file._
import org.openmole.core.tools.service.{ Scaling, Random }
import java.io.File
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage._
import org.openmole.core.updater.Updater
import org.openmole.core.workflow.job.Job
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.control._
import org.openmole.core.workspace._
import org.openmole.core.tools.service._
import org.openmole.core.batch.replication._
import org.openmole.tool.logger.Logger
import org.openmole.tool.thread._
import concurrent.stm._
import annotation.tailrec
import ref.WeakReference
import Scaling._
import Random._
import org.openmole.core.tools.service._
import fr.iscpif.gridscale.egi.{ GlobusAuthentication, WMSJobService, BDII }
import fr.iscpif.gridscale.RenewDecorator
import java.net.URI
import concurrent.duration._

object EGIEnvironment extends Logger {

  val ProxyTime = new ConfigurationLocation("EGIEnvironment", "ProxyTime")
  val MyProxyTime = new ConfigurationLocation("EGIEnvironment", "MyProxyTime")

  val FetchResourcesTimeOut = new ConfigurationLocation("EGIEnvironment", "FetchResourcesTimeOut")
  val CACertificatesSite = new ConfigurationLocation("EGIEnvironment", "CACertificatesSite")
  val CACertificatesCacheTime = new ConfigurationLocation("EGIEnvironment", "CACertificatesCacheTime")
  val CACertificatesDownloadTimeOut = new ConfigurationLocation("EGIEnvironment", "CACertificatesDownloadTimeOut")
  val VOInformationSite = new ConfigurationLocation("EGIEnvironment", "VOInformationSite")
  val VOCardDownloadTimeOut = new ConfigurationLocation("EGIEnvironment", "VOCardDownloadTimeOut")
  val VOCardCacheTime = new ConfigurationLocation("EGIEnvironment", "VOCardCacheTime")

  val EagerSubmissionInterval = new ConfigurationLocation("EGIEnvironment", "EagerSubmissionInterval")
  val EagerSubmissionMinNumberOfJob = new ConfigurationLocation("EGIEnvironment", "EagerSubmissionMinNumberOfJob")
  val EagerSubmissionNumberOfJobUnderMin = new ConfigurationLocation("EGIEnvironment", "EagerSubmissionNumberOfJobUnderMin")
  val EagerSubmissionNbSampling = new ConfigurationLocation("EGIEnvironment", "EagerSubmissionNbSampling")
  val EagerSubmissionSamplingWindowFactor = new ConfigurationLocation("EGIEnvironment", "EagerSubmissionSamplingWindowFactor")

  val LocalThreadsBySE = new ConfigurationLocation("EGIEnvironment", "LocalThreadsBySE")
  val LocalThreadsByWMS = new ConfigurationLocation("EGIEnvironment", "LocalThreadsByWMS")
  val MaxAccessesByMinuteWMS = new ConfigurationLocation("EGIEnvironment", "MaxAccessesByMinuteWMS")
  val MaxAccessesByMinuteSE = new ConfigurationLocation("EGIEnvironment", "MaxAccessesByMinuteSE")

  val ProxyRenewalRatio = new ConfigurationLocation("EGIEnvironment", "ProxyRenewalRatio")
  val MinProxyRenewal = new ConfigurationLocation("EGIEnvironment", "MinProxyRenewal")
  val JobShakingHalfLife = new ConfigurationLocation("EGIEnvironment", "JobShakingHalfLife")
  val JobShakingMaxReady = new ConfigurationLocation("EGIEnvironment", "JobShakingMaxReady")

  val RemoteCopyTimeout = new ConfigurationLocation("EGIEnvironment", "RemoteCopyTimeout")
  val QualityHysteresis = new ConfigurationLocation("EGIEnvironment", "QualityHysteresis")
  val MinValueForSelectionExploration = new ConfigurationLocation("EGIEnvironment", "MinValueForSelectionExploration")
  val ShallowWMSRetryCount = new ConfigurationLocation("EGIEnvironment", "ShallowWMSRetryCount")

  val JobServiceFitnessPower = ConfigurationLocation("EGIEnvironment", "JobServiceFitnessPower")
  val StorageFitnessPower = ConfigurationLocation("EGIEnvironment", "StorageFitnessPower")

  val StorageSizeFactor = ConfigurationLocation("EGIEnvironment", "StorageSizeFactor")
  val StorageTimeFactor = ConfigurationLocation("EGIEnvironment", "StorageTimeFactor")
  val StorageAvailabilityFactor = ConfigurationLocation("EGIEnvironment", "StorageAvailabilityFactor")
  val StorageSuccessRateFactor = ConfigurationLocation("EGIEnvironment", "StorageSuccessRateFactor")

  val JobServiceJobFactor = ConfigurationLocation("EGIEnvironment", "JobServiceSizeFactor")
  val JobServiceTimeFactor = ConfigurationLocation("EGIEnvironment", "JobServiceTimeFactor")
  val JobServiceAvailabilityFactor = ConfigurationLocation("EGIEnvironment", "JobServiceAvailabilityFactor")
  val JobServiceSuccessRateFactor = ConfigurationLocation("EGIEnvironment", "JobServiceSuccessRateFactor")

  val RunningHistoryDuration = ConfigurationLocation("EGIEnvironment", "RunningHistoryDuration")
  val EagerSubmissionThreshold = ConfigurationLocation("EGIEnvironment", "EagerSubmissionThreshold")

  val DefaultBDII = ConfigurationLocation("EGIEnvironment", "DefaultBDII")

  val EnvironmentCleaningThreads = ConfigurationLocation("EGIEnvironment", "EnvironmentCleaningThreads")

  val WMSRank = ConfigurationLocation("EGIEnvironment", "WMSRank")

  Workspace += (ProxyTime, "PT24H")
  Workspace += (MyProxyTime, "P7D")

  Workspace += (FetchResourcesTimeOut, "PT2M")
  Workspace += (CACertificatesSite, "http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/")
  Workspace += (CACertificatesCacheTime, "P7D")
  Workspace += (CACertificatesDownloadTimeOut, "PT2M")
  Workspace += (VOInformationSite, "http://operations-portal.egi.eu/xml/voIDCard/public/all/true")
  Workspace += (VOCardDownloadTimeOut, "PT2M")
  Workspace += (VOCardCacheTime, "PT6H")

  Workspace += (LocalThreadsBySE, "10")
  Workspace += (LocalThreadsByWMS, "10")
  Workspace += (MaxAccessesByMinuteWMS, "100")
  Workspace += (MaxAccessesByMinuteSE, "100")

  Workspace += (ProxyRenewalRatio, "0.2")
  Workspace += (MinProxyRenewal, "PT5M")

  Workspace += (EagerSubmissionNbSampling, "10")
  Workspace += (EagerSubmissionSamplingWindowFactor, "5")

  Workspace += (EagerSubmissionInterval, "PT5M")

  Workspace += (EagerSubmissionMinNumberOfJob, "100")
  Workspace += (EagerSubmissionNumberOfJobUnderMin, "10")

  Workspace += (JobShakingHalfLife, "PT30M")
  Workspace += (JobShakingMaxReady, "100")

  Workspace += (RemoteCopyTimeout, "PT10M")

  Workspace += (MinValueForSelectionExploration, "0.001")
  Workspace += (QualityHysteresis, "100")

  Workspace += (ShallowWMSRetryCount, "5")

  Workspace += (JobServiceFitnessPower, "2")
  Workspace += (StorageFitnessPower, "2")

  Workspace += (StorageSizeFactor, "5")
  Workspace += (StorageTimeFactor, "1")
  Workspace += (StorageAvailabilityFactor, "10")
  Workspace += (StorageSuccessRateFactor, "10")

  Workspace += (JobServiceJobFactor, "1")
  Workspace += (JobServiceTimeFactor, "10")
  Workspace += (JobServiceAvailabilityFactor, "10")
  Workspace += (JobServiceSuccessRateFactor, "1")

  Workspace += (RunningHistoryDuration, "PT12H")
  Workspace += (EagerSubmissionThreshold, "0.5")

  Workspace += (DefaultBDII, "ldap://cclcgtopbdii02.in2p3.fr:2170")

  Workspace += (EnvironmentCleaningThreads, "20")

  Workspace += (WMSRank, """( other.GlueCEStateFreeJobSlots > 0 ? other.GlueCEStateFreeJobSlots : (-other.GlueCEStateWaitingJobs * 4 / ( other.GlueCEStateRunningJobs + 1 )) - 1 )""")

  def apply(
    voName: String,
    bdii: Option[String] = None,
    vomsURL: Option[String] = None,
    fqan: Option[String] = None,
    openMOLEMemory: Option[Int] = None,
    memory: Option[Int] = None,
    cpuTime: Option[Duration] = None,
    wallTime: Option[Duration] = None,
    cpuNumber: Option[Int] = None,
    jobType: Option[String] = None,
    smpGranularity: Option[Int] = None,
    myProxy: Option[MyProxy] = None,
    architecture: Option[String] = None,
    threads: Option[Int] = None,
    requirements: Option[String] = None,
    debug: Boolean = false,
    name: Option[String] = None)(implicit authentications: AuthenticationProvider) =
    new EGIEnvironment(
      voName = voName,
      bdii = bdii.getOrElse(Workspace.preference(EGIEnvironment.DefaultBDII)),
      vomsURL = vomsURL.getOrElse(EGIAuthentication.getVMOSOrError(voName)),
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
      name = name)(authentications)

  def proxyTime = Workspace.preferenceAsDuration(ProxyTime)
  def proxyRenewalRatio = Workspace.preferenceAsDouble(EGIEnvironment.ProxyRenewalRatio)
  def proxyRenewalDelay = (proxyTime * proxyRenewalRatio) max Workspace.preferenceAsDuration(EGIEnvironment.MinProxyRenewal)

  def normalizedFitness[T](fitness: ⇒ Iterable[(T, Double)]): Iterable[(T, Double)] = {
    def orMinForExploration(v: Double) = {
      val min = Workspace.preferenceAsDouble(EGIEnvironment.MinValueForSelectionExploration)
      if (v < min) min else v
    }
    val fit = fitness
    val maxFit = fit.map(_._2).max
    fit.map { case (c, f) ⇒ (c, orMinForExploration(f / maxFit)) }
  }

}

class EGIBatchExecutionJob(val job: Job, val environment: EGIEnvironment) extends BatchExecutionJob {
  def selectStorage() = environment.selectAStorage(usedFileHashes)
  def selectJobService() = environment.selectAJobService
}

class EGIEnvironment(
    val voName: String,
    val bdii: String,
    val vomsURL: String,
    val fqan: Option[String],
    override val openMOLEMemory: Option[Int],
    val memory: Option[Int],
    val cpuTime: Option[Duration],
    val wallTime: Option[Duration],
    val cpuNumber: Option[Int],
    val jobType: Option[String],
    val smpGranularity: Option[Int],
    val myProxy: Option[MyProxy],
    val architecture: Option[String],
    override val threads: Option[Int],
    val requirements: Option[String],
    val debug: Boolean,
    override val name: Option[String])(implicit authentications: AuthenticationProvider) extends BatchEnvironment with MemoryRequirement with BDIIStorageServers with EGIEnvironmentId { env ⇒

  import EGIEnvironment._

  @transient lazy val threadsByWMS = Workspace.preferenceAsInt(LocalThreadsByWMS)

  type JS = EGIJobService

  @transient lazy val registerAgents = {
    Updater.delay(new EagerSubmissionAgent(WeakReference(this), EGIEnvironment.EagerSubmissionThreshold))
    None
  }

  def executionJob(job: Job) = new EGIBatchExecutionJob(job, this)

  override def submit(job: Job) = {
    registerAgents
    super.submit(job)
  }

  def proxyCreator = authentication

  @transient lazy val authentication = authentications(classOf[EGIAuthentication]).headOption match {
    case Some(a) ⇒
      EGIAuthentication.initialise(a)(
        vomsURL,
        voName,
        fqan)(authentications)
    case None ⇒ throw new UserBadDataError("No authentication has been initialized for EGI.")
  }

  @transient lazy val jobServices = {
    val bdiiWMS = bdiiServer.queryWMSLocations(voName, Workspace.preferenceAsDuration(FetchResourcesTimeOut))
    bdiiWMS.map {
      js ⇒
        new EGIJobService {
          val usageControl = new AvailabilityQuality with JobServiceQualityControl {
            override val usageControl: UsageControl = new LimitedAccess(threadsByWMS, Workspace.preferenceAsInt(MaxAccessesByMinuteWMS))
            override val hysteresis: Int = Workspace.preferenceAsInt(EGIEnvironment.QualityHysteresis)
          }

          val jobService = WMSJobService(js, threadsByWMS, proxyRenewalDelay)(authentication)
          val environment = env
        }
    }
  }

  def selectAJobService: (JobService, AccessToken) =
    jobServices match {
      case Nil       ⇒ throw new InternalProcessingError("No job service available for the environment.")
      case js :: Nil ⇒ (js, js.waitAToken)
      case _ ⇒
        def jobFactor(j: EGIJobService) = (j.usageControl.running.toDouble / j.usageControl.submitted) * (j.usageControl.totalDone.toDouble / j.usageControl.totalSubmitted)

        val times = jobServices.map(_.usageControl.time)
        val maxTime = times.max
        val minTime = times.min

        val jobFactors = jobServices.map(jobFactor)
        val maxJobFactor = jobFactors.max
        val minJobFactor = jobFactors.min

        @tailrec def select: (JobService, AccessToken) = {

          def fitness =
            for {
              cur ← jobServices
              if cur.available > 0
            } yield {
              val time = cur.usageControl.time

              val timeFactor =
                if (time.isNaN || maxTime.isNaN || minTime.isNaN || maxTime == 0.0) 0.0
                else 1 - time.normalize(minTime, maxTime)

              val jobFactor =
                if (cur.usageControl.submitted > 0 && cur.usageControl.totalSubmitted > 0) ((cur.usageControl.running.toDouble / cur.usageControl.submitted) * (cur.usageControl.totalDone / cur.usageControl.totalSubmitted)).normalize(minJobFactor, maxJobFactor)
                else 0.0

              import EGIEnvironment._

              val fitness = math.pow(
                Workspace.preferenceAsDouble(JobServiceJobFactor) * jobFactor +
                  Workspace.preferenceAsDouble(JobServiceTimeFactor) * timeFactor +
                  Workspace.preferenceAsDouble(JobServiceAvailabilityFactor) * cur.usageControl.availability +
                  Workspace.preferenceAsDouble(JobServiceSuccessRateFactor) * cur.usageControl.successRate,
                Workspace.preferenceAsDouble(JobServiceFitnessPower))
              (cur, fitness)
            }

          @tailrec def selected(value: Double, jobServices: List[(EGIJobService, Double)]): EGIJobService =
            jobServices match {
              case Nil                  ⇒ throw new InternalProcessingError("List should never be empty.")
              case (js, fitness) :: Nil ⇒ js
              case (js, fitness) :: tail ⇒
                if (value <= fitness) js
                else selected(value - fitness, tail)
            }

          val fs =
            atomic { implicit txn ⇒
              @tailrec def fit: Seq[(EGIJobService, Double)] =
                fitness match {
                  case Nil ⇒
                    retryFor(10000)
                    fit
                  case x ⇒ x
                }
              fit
            }

          val notLoaded = normalizedFitness(fs).shuffled(Random.default)
          val totalFitness = notLoaded.map { case (_, fitness) ⇒ fitness }.sum

          val jobService = selected(Random.default.nextDouble * totalFitness, notLoaded.toList)

          jobService.tryGetToken match {
            case Some(token) ⇒ jobService -> token
            case _           ⇒ select
          }

        }
        select
    }

  def bdiiServer: BDII = new BDII(bdii)
  override def runtimeSettings = super.runtimeSettings.copy(archiveResult = true)
}
