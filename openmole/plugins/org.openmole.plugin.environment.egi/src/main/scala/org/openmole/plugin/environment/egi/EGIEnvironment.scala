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

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.filedeleter.FileDeleter
import org.openmole.core.tools.io.FileUtil
import org.openmole.core.tools.service.{ Logger, Scaling, Random }
import java.io.File
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage._
import org.openmole.core.updater.Updater
import org.openmole.core.workflow.job.Job
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.control._
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation, AuthenticationProvider }
import org.openmole.core.tools.service._
import FileUtil._
import org.openmole.core.batch.replication._
import concurrent.stm._
import annotation.tailrec
import ref.WeakReference
import Scaling._
import Random._
import org.openmole.core.tools.service._
import fr.iscpif.gridscale.glite.{ GlobusAuthentication, WMSJobService, BDII }
import fr.iscpif.gridscale.RenewDecorator
import java.net.URI
import concurrent.duration._

object EGIEnvironment extends Logger {

  val ProxyTime = new ConfigurationLocation("EGIEnvironment", "ProxyTime")
  val MyProxyTime = new ConfigurationLocation("EGIEnvironment", "MyProxyTime")

  val FetchResourcesTimeOut = new ConfigurationLocation("EGIEnvironment", "FetchResourcesTimeOut")
  val CACertificatesSite = new ConfigurationLocation("EGIEnvironment", "CACertificatesSite")
  val CACertificatesCacheTime = new ConfigurationLocation("EGIEnvironment", "CACertificatesCacheTime")
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
  val ProxyRenewalRatio = new ConfigurationLocation("EGIEnvironment", "ProxyRenewalRatio")
  val MinProxyRenewal = new ConfigurationLocation("EGIEnvironment", "MinProxyRenewal")
  val JobShakingHalfLife = new ConfigurationLocation("EGIEnvironment", "JobShakingHalfLife")
  val JobShakingMaxReady = new ConfigurationLocation("EGIEnvironment", "JobShakingMaxReady")

  val RemoteTimeout = new ConfigurationLocation("EGIEnvironment", "RemoteTimeout")
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

  Workspace += (ProxyTime, "PT24H")
  Workspace += (MyProxyTime, "P7D")

  Workspace += (FetchResourcesTimeOut, "PT2M")
  Workspace += (CACertificatesSite, "http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/")
  Workspace += (CACertificatesCacheTime, "P7D")
  Workspace += (VOInformationSite, "http://operations-portal.egi.eu/xml/voIDCard/public/all/true")
  Workspace += (VOCardDownloadTimeOut, "PT2M")
  Workspace += (VOCardCacheTime, "P10D")

  Workspace += (LocalThreadsBySE, "10")
  Workspace += (LocalThreadsByWMS, "10")

  Workspace += (ProxyRenewalRatio, "0.2")
  Workspace += (MinProxyRenewal, "PT5M")

  Workspace += (EagerSubmissionNbSampling, "10")
  Workspace += (EagerSubmissionSamplingWindowFactor, "5")

  Workspace += (EagerSubmissionInterval, "PT5M")

  Workspace += (EagerSubmissionMinNumberOfJob, "100")
  Workspace += (EagerSubmissionNumberOfJobUnderMin, "10")

  Workspace += (JobShakingHalfLife, "PT30M")
  Workspace += (JobShakingMaxReady, "100")

  Workspace += (RemoteTimeout, "PT5M")

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
    debug: Boolean = false)(implicit authentications: AuthenticationProvider) =
    new EGIEnvironment(voName,
      bdii.getOrElse(Workspace.preference(EGIEnvironment.DefaultBDII)),
      vomsURL.getOrElse(EGIAuthentication.getVMOSOrError(voName)),
      fqan,
      openMOLEMemory,
      memory,
      cpuTime,
      wallTime,
      cpuNumber,
      jobType,
      smpGranularity,
      myProxy,
      architecture,
      threads,
      requirements,
      debug)(authentications)

  def proxyTime = Workspace.preferenceAsDuration(ProxyTime)

  def proxyRenewalDelay =
    (proxyTime * Workspace.preferenceAsDouble(EGIEnvironment.ProxyRenewalRatio)) max Workspace.preferenceAsDuration(EGIEnvironment.MinProxyRenewal)

  def normalizedFitness[T, S](fitness: ⇒ Iterable[(T, S, Double)]): Iterable[(T, S, Double)] = {
    def orMinForExploration(v: Double) = {
      val min = Workspace.preferenceAsDouble(EGIEnvironment.MinValueForSelectionExploration)
      if (v < min) min else v
    }
    val fit = fitness
    val maxFit = fit.map(_._3).max
    fit.map { case (c, t, f) ⇒ (c, t, orMinForExploration(f / maxFit)) }
  }

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
    val debug: Boolean)(implicit authentications: AuthenticationProvider) extends BatchEnvironment with MemoryRequirement with BDIISRMServers with EGIEnvironmentId with LCGCp { env ⇒

  import EGIEnvironment._

  @transient lazy val threadsByWMS = Workspace.preferenceAsInt(LocalThreadsByWMS)

  type JS = EGIJobService

  @transient lazy val registerAgents = {
    Updater.delay(new EagerSubmissionAgent(WeakReference(this), EGIEnvironment.EagerSubmissionThreshold))
    None
  }

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
        proxyTime,
        fqan)(authentications).cache(proxyRenewalDelay)
    case None ⇒ throw new UserBadDataError("No authentication has been initialized for EGI.")
  }

  @transient lazy val bdiiWMS = bdiiServer.queryWMS(voName, Workspace.preferenceAsDuration(FetchResourcesTimeOut))(authentication)

  override def allJobServices =
    bdiiWMS.map {
      js ⇒
        new EGIJobService {
          val jobService = new WMSJobService {
            val url = js.url
            val credential = js.credential
            override def connections = threadsByWMS
            override def delegationRenewal = proxyRenewalDelay
          }
          val environment = env
          val nbTokens = threadsByWMS
        }
    }

  override def selectAJobService =
    if (jobServices.size == 1) super.selectAJobService
    else {
      def jobFactor(j: EGIJobService) = (j.runnig.toDouble / j.submitted) * (j.totalDone.toDouble / j.totalSubmitted)

      val times = jobServices.map(_.time)
      val maxTime = times.max
      val minTime = times.min

      val jobFactors = jobServices.map(jobFactor)
      val maxJobFactor = jobFactors.max
      val minJobFactor = jobFactors.min

      def fitness =
        jobServices.flatMap {
          cur ⇒
            cur.tryGetToken match {
              case None ⇒ None
              case Some(token) ⇒
                val time = cur.time

                val timeFactor =
                  if (time.isNaN || maxTime.isNaN || minTime.isNaN || maxTime == 0.0) 0.0
                  else 1 - time.normalize(minTime, maxTime)

                val jobFactor =
                  if (cur.submitted > 0 && cur.totalSubmitted > 0) ((cur.runnig.toDouble / cur.submitted) * (cur.totalDone / cur.totalSubmitted)).normalize(minJobFactor, maxJobFactor)
                  else 0.0

                import Workspace.preferenceAsDouble
                import EGIEnvironment._

                val fitness = math.pow(
                  preferenceAsDouble(JobServiceJobFactor) * jobFactor +
                    preferenceAsDouble(JobServiceTimeFactor) * timeFactor +
                    preferenceAsDouble(JobServiceAvailabilityFactor) * cur.availability +
                    preferenceAsDouble(JobServiceSuccessRateFactor) * cur.successRate,
                  preferenceAsDouble(JobServiceFitnessPower))
                Some((cur, token, fitness))
            }
        }

      @tailrec def selected(value: Double, jobServices: List[(EGIJobService, AccessToken, Double)]): (EGIJobService, AccessToken) =
        jobServices match {
          case (js, token, fitness) :: Nil ⇒ (js, token)
          case (js, token, fitness) :: tail ⇒
            if (value <= fitness) (js, token)
            else selected(value - fitness, tail)
        }

      atomic { implicit txn ⇒
        val fitnesses = fitness

        if (!fitnesses.isEmpty) {
          val notLoaded = normalizedFitness(fitnesses).shuffled(Random.default)
          val totalFitness = notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum

          val (jobService, token) = selected(Random.default.nextDouble * totalFitness, notLoaded.toList)
          for {
            (s, t, _) ← notLoaded
            if (s.id != jobService.id)
          } s.releaseToken(t)
          jobService -> token
        }
        else retry

      }
    }

  def bdiiServer: BDII = new BDII(bdii)

}
