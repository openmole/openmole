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

import java.io.File

import org.openmole.core.communication.storage
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.plugin.environment.batch.storage.StorageInterface
import org.openmole.plugin.environment.egi.EGIEnvironment.WebDavLocation
import squants._
import squants.information._

//import java.net.URI
//
//import fr.iscpif.gridscale.egi.BDII
//import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
//import org.openmole.core.workflow.dsl._
//import org.openmole.core.workspace.Workspace
//import org.openmole.plugin.environment.batch.control._
//import org.openmole.plugin.environment.batch.environment._
//import org.openmole.tool.crypto.Cypher
import org.openmole.tool.logger.Logger
//import org.openmole.tool.random._
//import squants.information.Information
import squants.time.Time
import squants.time.TimeConversions._
import org.openmole.tool.cache._
import gridscale.egi._
import freedsl.dsl._

object EGIEnvironment extends Logger {

  import util._

  val FetchResourcesTimeOut = ConfigurationLocation("EGIEnvironment", "FetchResourcesTimeOut", Some(2 minutes))
  val CACertificatesSite = ConfigurationLocation("EGIEnvironment", "CACertificatesSite", Some("http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/"))
  val CACertificatesCacheTime = ConfigurationLocation("EGIEnvironment", "CACertificatesCacheTime", Some(7 days))
  val CACertificatesDownloadTimeOut = ConfigurationLocation("EGIEnvironment", "CACertificatesDownloadTimeOut", Some(2 minutes))
  val VOInformationSite = ConfigurationLocation("EGIEnvironment", "VOInformationSite", Some("http://operations-portal.egi.eu/xml/voIDCard/public/all/true"))
  val VOCardDownloadTimeOut = ConfigurationLocation("EGIEnvironment", "VOCardDownloadTimeOut", Some(2 minutes))
  val VOCardCacheTime = ConfigurationLocation("EGIEnvironment", "VOCardCacheTime", Some(6 hours))
  //
  //  val EagerSubmissionInterval = ConfigurationLocation("EGIEnvironment", "EagerSubmissionInterval", Some(2 minutes))
  //  val EagerSubmissionMinNumberOfJob = ConfigurationLocation("EGIEnvironment", "EagerSubmissionMinNumberOfJob", Some(100))
  //  val EagerSubmissionNumberOfJobUnderMin = ConfigurationLocation("EGIEnvironment", "EagerSubmissionNumberOfJobUnderMin", Some(10))
  //  val EagerSubmissionNbSampling = ConfigurationLocation("EGIEnvironment", "EagerSubmissionNbSampling", Some(10))
  //  val EagerSubmissionSamplingWindowFactor = ConfigurationLocation("EGIEnvironment", "EagerSubmissionSamplingWindowFactor", Some(5))
  //
  //  val ConnectionsBySRMSE = ConfigurationLocation("EGIEnvironment", "ConnectionsSRMSE", Some(10))
  //  val ConnectionsByWebDAVSE = ConfigurationLocation("EGIEnvironment", "ConnectionsByWebDAVSE", Some(100))
  //  val ConnectionsByWMS = ConfigurationLocation("EGIEnvironment", "ConnectionsByWMS", Some(10))
  //
  val ProxyLifeTime = ConfigurationLocation("EGIEnvironment", "ProxyTime", Some(24 hours))
  //  val MyProxyTime = ConfigurationLocation("EGIEnvironment", "MyProxyTime", Some(7 days))
  val ProxyRenewalTime = ConfigurationLocation("EGIEnvironment", "ProxyRenewalTime", Some(1 hours))
  val DIRACTokenRenewalMarginTime = ConfigurationLocation("EGIEnvironment", "ProxyRenewalTime", Some(1 hours))
  //  val JobShakingHalfLife = ConfigurationLocation("EGIEnvironment", "JobShakingHalfLife", Some(30 minutes))
  //  val JobShakingMaxReady = ConfigurationLocation("EGIEnvironment", "JobShakingMaxReady", Some(100))
  //
  //  val RemoteCopyTimeout = ConfigurationLocation("EGIEnvironment", "RemoteCopyTimeout", Some(10 minutes))
  //  val QualityHysteresis = ConfigurationLocation("EGIEnvironment", "QualityHysteresis", Some(100))
  //  val MinValueForSelectionExploration = ConfigurationLocation("EGIEnvironment", "MinValueForSelectionExploration", Some(0.001))
  //  val ShallowWMSRetryCount = ConfigurationLocation("EGIEnvironment", "ShallowWMSRetryCount", Some(5))
  //
  //  val JobServiceFitnessPower = ConfigurationLocation("EGIEnvironment", "JobServiceFitnessPower", Some(2.0))
  //  val StorageFitnessPower = ConfigurationLocation("EGIEnvironment", "StorageFitnessPower", Some(2.0))
  //
  //  val StorageSizeFactor = ConfigurationLocation("EGIEnvironment", "StorageSizeFactor", Some(5.0))
  //  val StorageTimeFactor = ConfigurationLocation("EGIEnvironment", "StorageTimeFactor", Some(1.0))
  //  val StorageAvailabilityFactor = ConfigurationLocation("EGIEnvironment", "StorageAvailabilityFactor", Some(10.0))
  //  val StorageSuccessRateFactor = ConfigurationLocation("EGIEnvironment", "StorageSuccessRateFactor", Some(10.0))
  //
  //  val JobServiceJobFactor = ConfigurationLocation("EGIEnvironment", "JobServiceSizeFactor", Some(1.0))
  //  val JobServiceTimeFactor = ConfigurationLocation("EGIEnvironment", "JobServiceTimeFactor", Some(10.0))
  //  val JobServiceAvailabilityFactor = ConfigurationLocation("EGIEnvironment", "JobServiceAvailabilityFactor", Some(10.0))
  //  val JobServiceSuccessRateFactor = ConfigurationLocation("EGIEnvironment", "JobServiceSuccessRateFactor", Some(1.0))
  //
  //  val RunningHistoryDuration = ConfigurationLocation("EGIEnvironment", "RunningHistoryDuration", Some(12 hours))
  //  val EagerSubmissionThreshold = ConfigurationLocation("EGIEnvironment", "EagerSubmissionThreshold", Some(0.5))

  val DiracConnectionTimeout = ConfigurationLocation("EGIEnvironment", "DiracConnectionTimeout", Some(1 minutes))
  val VOMSTimeout = ConfigurationLocation("EGIEnvironment", "VOMSTimeout", Some(1 minutes))

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

  def toBDII(bdii: java.net.URI)(implicit preference: Preference) = BDIIServer(bdii.getHost, bdii.getPort, preference(FetchResourcesTimeOut))

  def defaultBDIIs(implicit preference: Preference) = preference(EGIEnvironment.DefaultBDIIs).map(b ⇒ new java.net.URI(b)).map(toBDII)

  case class WebDavLocation(url: String)

  //  val EnvironmentCleaningThreads = ConfigurationLocation("EGIEnvironment", "EnvironmentCleaningThreads", Some(20))
  //
  //  val WMSRank = ConfigurationLocation("EGIEnvironment", "WMSRank", Some("""( other.GlueCEStateFreeJobSlots > 0 ? other.GlueCEStateFreeJobSlots : (-other.GlueCEStateWaitingJobs * 4 / ( other.GlueCEStateRunningJobs + 1 )) - 1 )"""))
  //

  //
  //  def normalizedFitness[T](fitness: ⇒ Iterable[(T, Double)], min: Double): Iterable[(T, Double)] = {
  //    def orMinForExploration(v: Double) = math.max(v, min)
  //    val fit = fitness
  //    val maxFit = fit.map(_._2).max
  //    if (maxFit < min) fit.map { case (c, _) ⇒ c → min }
  //    else fit.map { case (c, f) ⇒ c → orMinForExploration(f / maxFit) }
  //  }
  //
  //    def select[BS <: BatchService { def usageControl: AvailabilityQuality }](bss: List[BS], rate: BS ⇒ Double)(implicit preference: Preference, randomProvider: RandomProvider): Option[(BS, AccessToken)] =
  //      bss match {
  //        case Nil       ⇒ throw new InternalProcessingError("Cannot accept empty list.")
  //        case bs :: Nil ⇒ bs.tryGetToken.map(bs → _)
  //        case bss ⇒
  //          val (empty, nonEmpty) = bss.partition(_.usageControl.isEmpty)
  //
  //          def emptyFitness = empty.map { _ → 0.0 }
  //          def nonEmptyFitness = for { cur ← nonEmpty } yield cur → rate(cur)
  //          def fitness = nonEmptyFitness ++ emptyFitness
  //
  //          @tailrec def selected(value: Double, jobServices: List[(BS, Double)]): BS =
  //            jobServices match {
  //              case Nil                  ⇒ throw new InternalProcessingError("List should never be empty.")
  //              case (bs, fitness) :: Nil ⇒ bs
  //              case (bs, fitness) :: tail ⇒
  //                if (value <= fitness) bs
  //                else selected(value - fitness, tail)
  //            }
  //
  //          val notLoaded = normalizedFitness(fitness, preferencereum/e(EGIEnvironment.MinValueForSelectionExploration)).shuffled(randomProvider())
  //          val totalFitness = notLoaded.map { case (_, fitness) ⇒ fitness }.sum
  //
  //          val selectedBS = selected(randomProvider().nextDouble * totalFitness, notLoaded.toList)
  //
  //          selectedBS.tryGetToken.map(selectedBS → _)
  //      }
  //
  //  def apply(
  //    voName:         String,
  //    service:        OptionalArgument[String]      = None,
  //    group:          OptionalArgument[String]      = None,
  //    bdii:           OptionalArgument[String]      = None,
  //    vomsURLs:       OptionalArgument[Seq[String]] = None,
  //    setup:          OptionalArgument[String]      = None,
  //    fqan:           OptionalArgument[String]      = None,
  //    cpuTime:        OptionalArgument[Time]        = None,
  //    openMOLEMemory: OptionalArgument[Information] = None,
  //    debug:          Boolean                       = false,
  //    name:           OptionalArgument[String]      = None
  //  )(implicit authentication: EGIAuthentication, services: BatchEnvironment.Services, cypher: Cypher, workspace: Workspace, varName: sourcecode.Name) =
  //    DIRACEnvironment(
  //      voName = voName,
  //      service = service,
  //      group = group,
  //      bdii = bdii,
  //      vomsURLs = vomsURLs,
  //      setup = setup,
  //      fqan = fqan,
  //      cpuTime = cpuTime,
  //      openMOLEMemory = openMOLEMemory,
  //      debug = debug,
  //      name = name
  //    )(authentication, services, cypher, workspace, varName)
  //

  // implicit def egiAuthentication(implicit workspace: Workspace, authenticationStore: AuthenticationStore, serializerService: SerializerService): EGIAuthenticationInterface = EGIAuthenticationInterface().getOrElse(throw new UserBadDataError("No authentication was found"))

}

class EGIEnvironment[A: EGIAuthenticationInterface](
  val voName:         String,
  val service:        Option[String],
  val group:          Option[String],
  val bdiis:          Seq[gridscale.egi.BDIIServer],
  val vomsURLs:       Seq[String],
  val setup:          String,
  val fqan:           Option[String],
  val cpuTime:        Option[Time],
  val openMOLEMemory: Option[Information],
  val debug:          Boolean,
  val name:           Option[String],
  val authentication: A
)(implicit val services: BatchEnvironment.Services, workspace: Workspace) extends BatchEnvironment { env ⇒

  import services._

  implicit val interpreters = EGIInterpreter()
  import interpreters._

  lazy val proxyCache = TimeCache { () ⇒
    val proxy = EGIAuthentication.proxy(authentication, voName, fqan).get
    (proxy, preference(EGIEnvironment.ProxyRenewalTime))
  }

  lazy val tokenCache = TimeCache { () ⇒
    val token = EGIAuthentication.getToken(authentication, voName)
    (token, token.lifetime - preference(EGIEnvironment.DIRACTokenRenewalMarginTime))
  }

  override def usageControls = ???

  implicit def webdavlocationIsStorage = new StorageInterface[WebDavLocation] {
    def webdavServer(location: WebDavLocation) = gridscale.webdav.WebDAVSServer(location.url, proxyCache().factory)

    override def home(t: WebDavLocation): String = ""
    override def child(t: WebDavLocation, parent: String, child: String): String = gridscale.RemotePath.child(parent, child)
    override def parent(t: WebDavLocation, path: String): Option[String] = gridscale.RemotePath.parent(path)
    override def name(t: WebDavLocation, path: String): String = gridscale.RemotePath.name(path)

    override def exists(t: WebDavLocation, path: String): Boolean = gridscale.webdav.exists[DSL](webdavServer(t), path).eval
    override def list(t: WebDavLocation, path: String): Seq[gridscale.ListEntry] = gridscale.webdav.list[DSL](webdavServer(t), path).eval
    override def makeDir(t: WebDavLocation, path: String): Unit = gridscale.webdav.mkDirectory[DSL](webdavServer(t), path).eval
    override def rmDir(t: WebDavLocation, path: String): Unit = gridscale.webdav.rmDirectory[DSL](webdavServer(t), path).eval
    override def rmFile(t: WebDavLocation, path: String): Unit = gridscale.webdav.rmFile[DSL](webdavServer(t), path).eval
    override def mv(t: WebDavLocation, from: String, to: String): Unit = gridscale.webdav.mv[DSL](webdavServer(t), from, to).eval

    override def upload(t: WebDavLocation, src: File, dest: String, options: storage.TransferOptions): Unit =
      StorageInterface.upload(true, gridscale.webdav.writeStream[DSL](webdavServer(t), _, _).eval)(src, dest, options)
    override def download(t: WebDavLocation, src: String, dest: File, options: storage.TransferOptions): Unit =
      StorageInterface.download(true, gridscale.webdav.readStream[DSL, Unit](webdavServer(t), _, _).eval)(src, dest, options)

  }

  //webdav = WebDAVSServer(lal, proxy.factory)
  val storages = Cache {
    val webdavStorages = findWorking(bdiis, (b: BDIIServer) ⇒ webDAVs[DSL](b, voName).eval)
    if (!webdavStorages.isEmpty) webdavStorages.map(EGIEnvironment.WebDavLocation.apply)
    else throw new UserBadDataError("No WebDAV storage available for the VO")
    //      {
    //        //logger.fine("Use webdav storages:" + webdavStorages.mkString(","))
    //        implicit def preference = services.preference
    //        webdavStorages.map { s ⇒ EGIWebDAVStorageService(s, env, voName, debug, proxyCreator) }
    //      }
    //      else throw new UserBadDataError("No WebDAV storage available for the VO")
  }

  def storageService = {
    storages
  }

  override def trySelectStorage(files: ⇒ Vector[File]) = ???
  override def trySelectJobService() = ???
}