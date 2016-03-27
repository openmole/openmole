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
import org.openmole.core.fileservice.{ FileDeleter, FileService }
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

  val ProxyTime = ConfigurationLocation("EGIEnvironment", "ProxyTime", Some(24 hours))
  val MyProxyTime = ConfigurationLocation("EGIEnvironment", "MyProxyTime", Some(7 days))

  val FetchResourcesTimeOut = ConfigurationLocation("EGIEnvironment", "FetchResourcesTimeOut", Some(2 minutes))
  val CACertificatesSite = ConfigurationLocation("EGIEnvironment", "CACertificatesSite", Some("http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/"))
  val CACertificatesCacheTime = ConfigurationLocation("EGIEnvironment", "CACertificatesCacheTime", Some(7 days))
  val CACertificatesDownloadTimeOut = ConfigurationLocation("EGIEnvironment", "CACertificatesDownloadTimeOut", Some(2 minutes))
  val VOInformationSite = ConfigurationLocation("EGIEnvironment", "VOInformationSite", Some("http://operations-portal.egi.eu/xml/voIDCard/public/all/true"))
  val VOCardDownloadTimeOut = ConfigurationLocation("EGIEnvironment", "VOCardDownloadTimeOut", Some(2 minutes))
  val VOCardCacheTime = ConfigurationLocation("EGIEnvironment", "VOCardCacheTime", Some(6 hours))

  val EagerSubmissionInterval = ConfigurationLocation("EGIEnvironment", "EagerSubmissionInterval", Some(2 minutes))
  val EagerSubmissionMinNumberOfJob = ConfigurationLocation("EGIEnvironment", "EagerSubmissionMinNumberOfJob", Some(100))
  val EagerSubmissionNumberOfJobUnderMin = ConfigurationLocation("EGIEnvironment", "EagerSubmissionNumberOfJobUnderMin", Some(10))
  val EagerSubmissionNbSampling = ConfigurationLocation("EGIEnvironment", "EagerSubmissionNbSampling", Some(10))
  val EagerSubmissionSamplingWindowFactor = ConfigurationLocation("EGIEnvironment", "EagerSubmissionSamplingWindowFactor", Some(5))

  val ConnectionsBySRMSE = ConfigurationLocation("EGIEnvironment", "ConnectionsSRMSE", Some(10))
  val ConnectionsByWebDAVSE = ConfigurationLocation("EGIEnvironment", "ConnectionsByWebDAVSE", Some(10))
  val ConnectionsByWMS = ConfigurationLocation("EGIEnvironment", "ConnectionsByWMS", Some(10))

  val ProxyRenewalRatio = ConfigurationLocation("EGIEnvironment", "ProxyRenewalRatio", Some(0.2))
  val MinProxyRenewal = ConfigurationLocation("EGIEnvironment", "MinProxyRenewal", Some(5 minutes))
  val JobShakingHalfLife = ConfigurationLocation("EGIEnvironment", "JobShakingHalfLife", Some(30 minutes))
  val JobShakingMaxReady = ConfigurationLocation("EGIEnvironment", "JobShakingMaxReady", Some(100))

  val RemoteCopyTimeout = ConfigurationLocation("EGIEnvironment", "RemoteCopyTimeout", Some(10 minutes))
  val QualityHysteresis = ConfigurationLocation("EGIEnvironment", "QualityHysteresis", Some(100))
  val MinValueForSelectionExploration = ConfigurationLocation("EGIEnvironment", "MinValueForSelectionExploration", Some(0.001))
  val ShallowWMSRetryCount = ConfigurationLocation("EGIEnvironment", "ShallowWMSRetryCount", Some(5))

  val JobServiceFitnessPower = ConfigurationLocation("EGIEnvironment", "JobServiceFitnessPower", Some(2.0))
  val StorageFitnessPower = ConfigurationLocation("EGIEnvironment", "StorageFitnessPower", Some(2.0))

  val StorageSizeFactor = ConfigurationLocation("EGIEnvironment", "StorageSizeFactor", Some(5.0))
  val StorageTimeFactor = ConfigurationLocation("EGIEnvironment", "StorageTimeFactor", Some(1.0))
  val StorageAvailabilityFactor = ConfigurationLocation("EGIEnvironment", "StorageAvailabilityFactor", Some(10.0))
  val StorageSuccessRateFactor = ConfigurationLocation("EGIEnvironment", "StorageSuccessRateFactor", Some(10.0))

  val JobServiceJobFactor = ConfigurationLocation("EGIEnvironment", "JobServiceSizeFactor", Some(1.0))
  val JobServiceTimeFactor = ConfigurationLocation("EGIEnvironment", "JobServiceTimeFactor", Some(10.0))
  val JobServiceAvailabilityFactor = ConfigurationLocation("EGIEnvironment", "JobServiceAvailabilityFactor", Some(10.0))
  val JobServiceSuccessRateFactor = ConfigurationLocation("EGIEnvironment", "JobServiceSuccessRateFactor", Some(1.0))

  val RunningHistoryDuration = ConfigurationLocation("EGIEnvironment", "RunningHistoryDuration", Some(12 hours))
  val EagerSubmissionThreshold = ConfigurationLocation("EGIEnvironment", "EagerSubmissionThreshold", Some(0.5))

  val DefaultBDII = ConfigurationLocation("EGIEnvironment", "DefaultBDII", Some("ldap://cclcgtopbdii02.in2p3.fr:2170"))

  val EnvironmentCleaningThreads = ConfigurationLocation("EGIEnvironment", "EnvironmentCleaningThreads", Some(20))

  val WMSRank = ConfigurationLocation("EGIEnvironment", "WMSRank", Some("""( other.GlueCEStateFreeJobSlots > 0 ? other.GlueCEStateFreeJobSlots : (-other.GlueCEStateWaitingJobs * 4 / ( other.GlueCEStateRunningJobs + 1 )) - 1 )"""))

  Workspace setDefault ProxyTime
  Workspace setDefault MyProxyTime

  Workspace setDefault FetchResourcesTimeOut
  Workspace setDefault CACertificatesSite
  Workspace setDefault CACertificatesCacheTime
  Workspace setDefault CACertificatesDownloadTimeOut
  Workspace setDefault VOInformationSite
  Workspace setDefault VOCardDownloadTimeOut
  Workspace setDefault VOCardCacheTime

  Workspace setDefault ConnectionsBySRMSE
  Workspace setDefault ConnectionsByWMS
  Workspace setDefault ConnectionsByWebDAVSE

  Workspace setDefault ProxyRenewalRatio
  Workspace setDefault MinProxyRenewal

  Workspace setDefault EagerSubmissionNbSampling
  Workspace setDefault EagerSubmissionSamplingWindowFactor

  Workspace setDefault EagerSubmissionInterval

  Workspace setDefault EagerSubmissionMinNumberOfJob
  Workspace setDefault EagerSubmissionNumberOfJobUnderMin

  Workspace setDefault JobShakingHalfLife
  Workspace setDefault JobShakingMaxReady

  Workspace setDefault RemoteCopyTimeout

  Workspace setDefault MinValueForSelectionExploration
  Workspace setDefault QualityHysteresis

  Workspace setDefault ShallowWMSRetryCount
  Workspace setDefault JobServiceFitnessPower
  Workspace setDefault StorageFitnessPower

  Workspace setDefault StorageSizeFactor
  Workspace setDefault StorageTimeFactor
  Workspace setDefault StorageAvailabilityFactor
  Workspace setDefault StorageSuccessRateFactor

  Workspace setDefault JobServiceJobFactor
  Workspace setDefault JobServiceTimeFactor
  Workspace setDefault JobServiceAvailabilityFactor
  Workspace setDefault JobServiceSuccessRateFactor

  Workspace setDefault RunningHistoryDuration
  Workspace setDefault EagerSubmissionThreshold

  Workspace setDefault DefaultBDII

  Workspace setDefault EnvironmentCleaningThreads

  Workspace setDefault WMSRank

  def apply(
    voName:         String,
    bdii:           Option[String]      = None,
    vomsURLs:       Option[Seq[String]] = None,
    fqan:           Option[String]      = None,
    openMOLEMemory: Option[Int]         = None,
    memory:         Option[Int]         = None,
    cpuTime:        Option[Duration]    = None,
    wallTime:       Option[Duration]    = None,
    cpuNumber:      Option[Int]         = None,
    jobType:        Option[String]      = None,
    smpGranularity: Option[Int]         = None,
    myProxy:        Option[MyProxy]     = None,
    architecture:   Option[String]      = None,
    threads:        Option[Int]         = None,
    requirements:   Option[String]      = None,
    debug:          Boolean             = false,
    name:           Option[String]      = None
  )(implicit authentication: EGIAuthentication, uncypher: Decrypt) =
    new EGIEnvironment(
      voName = voName,
      bdii = bdii.map(s ⇒ new URI(s)).getOrElse(new URI(Workspace.preference(EGIEnvironment.DefaultBDII))),
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

  def proxyTime = Workspace.preference(ProxyTime)
  def proxyRenewalRatio = Workspace.preference(EGIEnvironment.ProxyRenewalRatio)
  def proxyRenewalDelay = (proxyTime * proxyRenewalRatio) max Workspace.preference(EGIEnvironment.MinProxyRenewal)

  def normalizedFitness[T](fitness: ⇒ Iterable[(T, Double)], min: Double = Workspace.preference(EGIEnvironment.MinValueForSelectionExploration)): Iterable[(T, Double)] = {
    def orMinForExploration(v: Double) = math.max(v, min)
    val fit = fitness
    val maxFit = fit.map(_._2).max
    if (maxFit < min) fit.map { case (c, _) ⇒ c → min }
    else fit.map { case (c, f) ⇒ c → orMinForExploration(f / maxFit) }
  }

  def select[BS <: BatchService { def usageControl: AvailabilityQuality }](bss: List[BS], rate: BS ⇒ Double): Option[(BS, AccessToken)] =
    bss match {
      case Nil       ⇒ throw new InternalProcessingError("Cannot accept empty list.")
      case bs :: Nil ⇒ bs.tryGetToken.map(bs → _)
      case bss ⇒
        val (empty, nonEmpty) = bss.partition(_.usageControl.isEmpty)

        def emptyFitness = empty.map { _ → 0.0 }
        def nonEmptyFitness = for { cur ← nonEmpty } yield cur → rate(cur)
        def fitness = nonEmptyFitness ++ emptyFitness

        @tailrec def selected(value: Double, jobServices: List[(BS, Double)]): BS =
          jobServices match {
            case Nil                  ⇒ throw new InternalProcessingError("List should never be empty.")
            case (bs, fitness) :: Nil ⇒ bs
            case (bs, fitness) :: tail ⇒
              if (value <= fitness) bs
              else selected(value - fitness, tail)
          }

        val notLoaded = normalizedFitness(fitness).shuffled(Workspace.rng)
        val totalFitness = notLoaded.map { case (_, fitness) ⇒ fitness }.sum

        val selectedBS = selected(Workspace.rng.nextDouble * totalFitness, notLoaded.toList)

        selectedBS.tryGetToken.map(selectedBS → _)
    }

}

class EGIBatchExecutionJob(val job: Job, val environment: EGIEnvironment) extends BatchExecutionJob {
  def trySelectStorage() = environment.trySelectAStorage(usedFileHashes)
  def trySelectJobService() = environment.trySelectAJobService
}

class EGIEnvironment(
    val voName:                  String,
    val bdii:                    URI,
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

  @transient lazy val connectionsByWMS = Workspace.preference(ConnectionsByWMS)

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
      fqan
    )(decrypt)

  @transient lazy val jobServices = {
    val bdiiWMS = bdiiServer.queryWMSLocations(voName)
    bdiiWMS.map {
      js ⇒
        new EGIJobService {
          val usageControl = new AvailabilityQuality with JobServiceQualityControl {
            override val usageControl = new LimitedAccess(connectionsByWMS, Int.MaxValue)
            override val hysteresis = Workspace.preference(EGIEnvironment.QualityHysteresis)
          }

          val jobService = WMSJobService(js, connectionsByWMS, proxyRenewalDelay)(authentication)
          def environment = env
        }
    }
  }

  def trySelectAJobService = {
    val jss = jobServices
    if (jss.isEmpty) throw new InternalProcessingError("No job service available for the environment.")

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

  def bdiiServer: BDII = BDII(bdii.getHost, bdii.getPort, Workspace.preference(FetchResourcesTimeOut))
  override def runtimeSettings = super.runtimeSettings.copy(archiveResult = true)
}
