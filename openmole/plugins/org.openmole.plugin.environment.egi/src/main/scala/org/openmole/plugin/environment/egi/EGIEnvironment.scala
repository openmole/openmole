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

import java.net.URI

import fr.iscpif.gridscale.egi.BDII
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.dsl._
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.control._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.tool.logger.Logger
import org.openmole.tool.random._

import scala.annotation.tailrec
import scala.concurrent.duration._

object EGIEnvironment extends Logger {

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
  val ConnectionsByWebDAVSE = ConfigurationLocation("EGIEnvironment", "ConnectionsByWebDAVSE", Some(100))
  val ConnectionsByWMS = ConfigurationLocation("EGIEnvironment", "ConnectionsByWMS", Some(10))

  val ProxyTime = ConfigurationLocation("EGIEnvironment", "ProxyTime", Some(24 hours))
  val MyProxyTime = ConfigurationLocation("EGIEnvironment", "MyProxyTime", Some(7 days))
  val ProxyRenewalTime = ConfigurationLocation("EGIEnvironment", "ProxyRenewalTime", Some(20 minutes))
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

  val ldapURLs =
    Seq(
      "topbdii.grif.fr",
      "bdii.ndgf.org",
      "lcg-bdii.cern.ch",
      "bdii-fzk.gridka.de",
      "topbdii.egi.cesga.es",
      "egee-bdii.cnaf.infn.it"
    ).map(h ⇒ s"ldap://$h:2170")

  val DefaultBDIIs = ConfigurationLocation("EGIEnvironment", "DefaultBDIIs", Some(ldapURLs))

  def toBDII(bdii: URI) = BDII(bdii.getHost, bdii.getPort, Workspace.preference(FetchResourcesTimeOut))
  def defaultBDIIs = Workspace.preference(EGIEnvironment.DefaultBDIIs).map(b ⇒ new URI(b)).map(toBDII)

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

  Workspace setDefault ProxyRenewalTime

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

  Workspace setDefault DefaultBDIIs

  Workspace setDefault EnvironmentCleaningThreads

  Workspace setDefault WMSRank

  def proxyTime = Workspace.preference(ProxyTime)
  def proxyRenewalTime = Workspace.preference(EGIEnvironment.ProxyRenewalTime)

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
    DIRACEnvironment(
      voName = voName,
      service = service,
      group = group,
      bdii = bdii,
      vomsURLs = vomsURLs,
      setup = setup,
      fqan = fqan,
      cpuTime = cpuTime,
      openMOLEMemory = openMOLEMemory,
      debug = debug,
      name = name
    )(authentication, decrypt)

}
