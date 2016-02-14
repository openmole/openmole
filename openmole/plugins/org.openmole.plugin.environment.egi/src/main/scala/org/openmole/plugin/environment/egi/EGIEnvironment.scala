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
import org.openmole.core.fileservice.{FileDeleter, FileService}
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

  val ConnectionsBySRMSE = new ConfigurationLocation("EGIEnvironment", "ConnectionsSRMSE")
  val ConnectionsByWebDAVSE = new ConfigurationLocation("EGIEnvironment", "ConnectionsByWebDAVSE")
  val ConnectionsByWMS = new ConfigurationLocation("EGIEnvironment", "ConnectionsByWMS")

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

  Workspace += (ConnectionsBySRMSE, "10")
  Workspace += (ConnectionsByWMS, "10")
  Workspace += (ConnectionsByWebDAVSE, "10")

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
    vomsURLs: Option[Seq[String]] = None,
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
    name: Option[String] = None)(implicit authentication: EGIAuthentication, uncypher: Decrypt) =
    new EGIEnvironment(
      voName = voName,
      bdii = bdii.map(s => new URI(s)).getOrElse(new URI(Workspace.preference(EGIEnvironment.DefaultBDII))),
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
      name = name)(authentication, uncypher)

  def proxyTime = Workspace.preferenceAsDuration(ProxyTime)
  def proxyRenewalRatio = Workspace.preferenceAsDouble(EGIEnvironment.ProxyRenewalRatio)
  def proxyRenewalDelay = (proxyTime * proxyRenewalRatio) max Workspace.preferenceAsDuration(EGIEnvironment.MinProxyRenewal)

  def normalizedFitness[T](fitness: ⇒ Iterable[(T, Double)], min: Double = Workspace.preferenceAsDouble(EGIEnvironment.MinValueForSelectionExploration)): Iterable[(T, Double)] = {
    def orMinForExploration(v: Double) = math.max(v, min)
    val fit = fitness
    val maxFit = fit.map(_._2).max
    if(maxFit < min) fit.map{ case(c, _) => c -> min  }
    else fit.map { case (c, f) ⇒ c -> orMinForExploration(f / maxFit) }
  }


  def select[BS <: BatchService { def usageControl: AvailabilityQuality}](bss: List[BS], rate: BS => Double): Option[(BS, AccessToken)] =
    bss match {
      case Nil       ⇒ throw new InternalProcessingError("Cannot accept empty list.")
      case bs :: Nil ⇒ bs.tryGetToken.map(bs -> _)
      case bss ⇒
        val (empty, nonEmpty) = bss.partition(_.usageControl.isEmpty)

        def emptyFitness = empty.map { _ -> 0.0 }
        def nonEmptyFitness = for { cur ← nonEmpty } yield cur -> rate(cur)
        def fitness = nonEmptyFitness ++ emptyFitness

        @tailrec def selected(value: Double, jobServices: List[(BS, Double)]): BS =
          jobServices match {
            case Nil                  ⇒ throw new InternalProcessingError("List should never be empty.")
            case (bs, fitness) :: Nil ⇒ bs
            case (bs, fitness) :: tail ⇒
              if (value <= fitness) bs
              else selected(value - fitness, tail)
          }

        val notLoaded = normalizedFitness(fitness).shuffled(Random.default)
        val totalFitness = notLoaded.map { case (_, fitness) ⇒ fitness }.sum

        val selectedBS = selected(Random.default.nextDouble * totalFitness, notLoaded.toList)

        selectedBS.tryGetToken.map(selectedBS -> _)
    }

}

class EGIBatchExecutionJob(val job: Job, val environment: EGIEnvironment) extends BatchExecutionJob {
  def trySelectStorage() = environment.trySelectAStorage(usedFileHashes)
  def trySelectJobService() = environment.trySelectAJobService
}

class EGIEnvironment(
    val voName: String,
    val bdii: URI,
    val vomsURLs: Seq[String],
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
    override val name: Option[String])(implicit a: EGIAuthentication, decrypt: Decrypt) extends BatchEnvironment with MemoryRequirement with BDIIStorageServers with EGIEnvironmentId { env ⇒

  import EGIEnvironment._

  @transient lazy val connectionsByWMS = Workspace.preferenceAsInt(ConnectionsByWMS)

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

  @transient lazy val authentication =
      EGIAuthentication.initialise(a)(
        vomsURLs,
        voName,
        fqan)(decrypt)


  @transient lazy val jobServices = {
    val bdiiWMS = bdiiServer.queryWMSLocations(voName)
    bdiiWMS.map {
      js ⇒
        new EGIJobService {
          val usageControl = new AvailabilityQuality with JobServiceQualityControl {
            override val usageControl = new LimitedAccess(connectionsByWMS, Int.MaxValue)
            override val hysteresis = Workspace.preferenceAsInt(EGIEnvironment.QualityHysteresis)
          }

          val jobService = WMSJobService(js, connectionsByWMS, proxyRenewalDelay)(authentication)
          def environment = env
        }
    }
  }

  def trySelectAJobService = {
    val jss = jobServices
    if(jss.isEmpty) throw new InternalProcessingError("No job service available for the environment.")

    val nonEmpty = jss.filter(!_.usageControl.isEmpty)
    def jobFactor(j: EGIJobService) = (j.usageControl.running.toDouble / j.usageControl.submitted) * (j.usageControl.totalDone.toDouble / j.usageControl.totalSubmitted)

    lazy val times = nonEmpty.map(_.usageControl.time)
    lazy val maxTime = times.max
    lazy val minTime = times.min

    lazy val availablities = nonEmpty.map(_.usageControl.availability)
    lazy val maxAvailability = availablities.max
    lazy val minAvailability = availablities.min

    lazy val jobFactors = nonEmpty.map(jobFactor)
    lazy val maxJobFactor = jobFactors.max
    lazy val minJobFactor = jobFactors.min

    def rate(js: EGIJobService) = {
      val time = js.usageControl.time
      val timeFactor = if (minTime == maxTime) 1.0 else 1.0 - time.normalize(minTime, maxTime)

      val availability = js.usageControl.availability
      val availabilityFactor = if(minAvailability == maxAvailability) 1.0 else 1.0 - availability.normalize(minTime, maxTime)

      val jobFactor =
        if (js.usageControl.submitted > 0 && js.usageControl.totalSubmitted > 0) ((js.usageControl.running.toDouble / js.usageControl.submitted) * (js.usageControl.totalDone / js.usageControl.totalSubmitted)).normalize(minJobFactor, maxJobFactor)
        else 0.0

      math.pow(
        Workspace.preferenceAsDouble(JobServiceJobFactor) * jobFactor +
        Workspace.preferenceAsDouble(JobServiceTimeFactor) * timeFactor +
        Workspace.preferenceAsDouble(JobServiceAvailabilityFactor) * availability +
        Workspace.preferenceAsDouble(JobServiceSuccessRateFactor) * js.usageControl.successRate,
        Workspace.preferenceAsDouble(JobServiceFitnessPower))
    }

    select(jss.toList, rate)
  }

  def bdiiServer: BDII = BDII(bdii.getHost, bdii.getPort, Workspace.preferenceAsDuration(FetchResourcesTimeOut))
  override def runtimeSettings = super.runtimeSettings.copy(archiveResult = true)
}
