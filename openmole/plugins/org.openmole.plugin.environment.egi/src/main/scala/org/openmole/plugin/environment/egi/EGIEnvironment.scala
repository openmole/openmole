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

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.communication.storage
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.threadprovider.Updater
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, SerializedJob, UsageControl }
import org.openmole.plugin.environment.batch.jobservice.{ BatchJob, BatchJobService, JobServiceInterface }
import org.openmole.plugin.environment.batch.refresh.{ JobManager, StopEnvironment }
import org.openmole.plugin.environment.batch.storage.{ StorageInterface, StorageService }
import org.openmole.plugin.environment.egi.EGIEnvironment.WebDavLocation
import org.openmole.tool.crypto.Cypher
import squants._
import squants.information._

import scala.ref.WeakReference

//import java.net.URI
//
//import fr.iscpif.gridscale.egi.BDII
//import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.workflow.dsl._
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
import effectaside._

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
  val EagerSubmissionInterval = ConfigurationLocation("EGIEnvironment", "EagerSubmissionInterval", Some(2 minutes))
  val EagerSubmissionMinNumberOfJobs = ConfigurationLocation("EGIEnvironment", "EagerSubmissionMinNumberOfJobs", Some(100))
  val RunningHistoryDuration = ConfigurationLocation("EGIEnvironment", "RunningHistoryDuration", Some(12 hours))
  val EagerSubmissionThreshold = ConfigurationLocation("EGIEnvironment", "EagerSubmissionThreshold", Some(0.5))
  val EagerSubmissionNumberOfJobs = ConfigurationLocation("EGIEnvironment", "EagerSubmissionNumberOfJobs", Some(5))

  //  val EagerSubmissionNbSampling = ConfigurationLocation("EGIEnvironment", "EagerSubmissionNbSampling", Some(10))
  //  val EagerSubmissionSamplingWindowFactor = ConfigurationLocation("EGIEnvironment", "EagerSubmissionSamplingWindowFactor", Some(5))
  //
  //  val ConnectionsBySRMSE = ConfigurationLocation("EGIEnvironment", "ConnectionsSRMSE", Some(10))
  val ConnexionsByWebDAVSE = ConfigurationLocation("EGIEnvironment", "ConnectionsByWebDAVSE", Some(100))
  val ConnectionsToDIRAC = ConfigurationLocation("EGIEnvironment", "ConnectionsToDIRAC", Some(10))
  //
  val ProxyLifeTime = ConfigurationLocation("EGIEnvironment", "ProxyTime", Some(24 hours))
  //  val MyProxyTime = ConfigurationLocation("EGIEnvironment", "MyProxyTime", Some(7 days))
  val ProxyRenewalTime = ConfigurationLocation("EGIEnvironment", "ProxyRenewalTime", Some(1 hours))
  val TokenRenewalMarginTime = ConfigurationLocation("EGIEnvironment", "TokenRenewalMarginTime", Some(1 hours))
  val JobGroupRefreshInterval = ConfigurationLocation("EGIEnvironment", "JobGroupRefreshInterval", Some(1 minutes))
  //  val JobShakingHalfLife = ConfigurationLocation("EGIEnvironment", "JobShakingHalfLife", Some(30 minutes))
  //  val JobShakingMaxReady = ConfigurationLocation("EGIEnvironment", "JobShakingMaxReady", Some(100))
  //
  val RemoteCopyTimeout = ConfigurationLocation("EGIEnvironment", "RemoteCopyTimeout", Some(30 minutes))
  //  val QualityHysteresis = ConfigurationLocation("EGIEnvironment", "QualityHysteresis", Some(100))
  val MinValueForSelectionExploration = ConfigurationLocation("EGIEnvironment", "MinValueForSelectionExploration", Some(0.001))
  //  val ShallowWMSRetryCount = ConfigurationLocation("EGIEnvironment", "ShallowWMSRetryCount", Some(5))
  //
  //  val JobServiceFitnessPower = ConfigurationLocation("EGIEnvironment", "JobServiceFitnessPower", Some(2.0))
  val StorageFitnessPower = ConfigurationLocation("EGIEnvironment", "StorageFitnessPower", Some(2.0))
  //
  val StorageSizeFactor = ConfigurationLocation("EGIEnvironment", "StorageSizeFactor", Some(5.0))
  val StorageTimeFactor = ConfigurationLocation("EGIEnvironment", "StorageTimeFactor", Some(1.0))
  //    val StorageAvailabilityFactor = ConfigurationLocation("EGIEnvironment", "StorageAvailabilityFactor", Some(10.0))
  val StorageSuccessRateFactor = ConfigurationLocation("EGIEnvironment", "StorageSuccessRateFactor", Some(10.0))

  //  val JobServiceJobFactor = ConfigurationLocation("EGIEnvironment", "JobServiceSizeFactor", Some(1.0))
  //  val JobServiceTimeFactor = ConfigurationLocation("EGIEnvironment", "JobServiceTimeFactor", Some(10.0))
  //  val JobServiceAvailabilityFactor = ConfigurationLocation("EGIEnvironment", "JobServiceAvailabilityFactor", Some(10.0))
  //  val JobServiceSuccessRateFactor = ConfigurationLocation("EGIEnvironment", "JobServiceSuccessRateFactor", Some(1.0))
  //

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

  def stdOutFileName = "output"
  def stdErrFileName = "error"

  implicit def isJobService[A]: JobServiceInterface[EGIEnvironment[A]] = new JobServiceInterface[EGIEnvironment[A]] {
    override type J = gridscale.dirac.JobID
    override def submit(env: EGIEnvironment[A], serializedJob: SerializedJob): BatchJob[J] = env.submit(serializedJob)
    override def state(env: EGIEnvironment[A], j: J) = env.state(j)
    override def delete(env: EGIEnvironment[A], j: J): Unit = env.delete(j)
    override def stdOutErr(env: EGIEnvironment[A], j: J) = env.stdOutErr(j)
  }

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

  //      def select[BS <: { def usageControl: AvailabilityQuality }](bss: List[BS], rate: BS ⇒ Double)(implicit preference: Preference, randomProvider: RandomProvider): Option[(BS, AccessToken)] =
  //        bss match {
  //          case Nil       ⇒ throw new InternalProcessingError("Cannot accept empty list.")
  //          case bs :: Nil ⇒ bs.tryGetToken.map(bs → _)
  //          case bss ⇒
  //            val (empty, nonEmpty) = bss.partition(_.usageControl.isEmpty)
  //
  //            def emptyFitness = empty.map { _ → 0.0 }
  //            def nonEmptyFitness = for { cur ← nonEmpty } yield cur → rate(cur)
  //            def fitness = nonEmptyFitness ++ emptyFitness
  //
  //            @tailrec def selected(value: Double, jobServices: List[(BS, Double)]): BS =
  //              jobServices match {
  //                case Nil                  ⇒ throw new InternalProcessingError("List should never be empty.")
  //                case (bs, fitness) :: Nil ⇒ bs
  //                case (bs, fitness) :: tail ⇒
  //                  if (value <= fitness) bs
  //                  else selected(value - fitness, tail)
  //              }
  //
  //            val notLoaded = normalizedFitness(fitness, preference(EGIEnvironment.MinValueForSelectionExploration)).shuffled(randomProvider())
  //            val totalFitness = notLoaded.map { case (_, fitness) ⇒ fitness }.sum
  //
  //            val selectedBS = selected(randomProvider().nextDouble * totalFitness, notLoaded.toList)
  //
  //            selectedBS.tryGetToken.map(selectedBS → _)
  //        }
  //
  def apply(
    voName:         String,
    service:        OptionalArgument[String]      = None,
    group:          OptionalArgument[String]      = None,
    bdii:           OptionalArgument[String]      = None,
    vomsURLs:       OptionalArgument[Seq[String]] = None,
    fqan:           OptionalArgument[String]      = None,
    cpuTime:        OptionalArgument[Time]        = None,
    openMOLEMemory: OptionalArgument[Information] = None,
    debug:          Boolean                       = false,
    name:           OptionalArgument[String]      = None
  )(implicit authentication: EGIAuthentication, services: BatchEnvironment.Services, cypher: Cypher, workspace: Workspace, varName: sourcecode.Name) = {

    new EGIEnvironment(
      voName = voName,
      service = service,
      group = group,
      bdiiURL = bdii,
      vomsURLs = vomsURLs,
      fqan = fqan,
      cpuTime = cpuTime,
      openMOLEMemory = openMOLEMemory,
      debug = debug,
      name = name,
      authentication = authentication
    )
  }

}

class EGIEnvironment[A: EGIAuthenticationInterface](
  val voName:         String,
  val service:        Option[String],
  val group:          Option[String],
  val bdiiURL:        Option[String],
  val vomsURLs:       Option[Seq[String]],
  val fqan:           Option[String],
  val cpuTime:        Option[Time],
  val openMOLEMemory: Option[Information],
  val debug:          Boolean,
  val name:           Option[String],
  val authentication: A
)(implicit val services: BatchEnvironment.Services, workspace: Workspace) extends BatchEnvironment { env ⇒

  import services._

  lazy val eagerSubmissionAgent = new EagerSubmissionAgent(WeakReference(this))

  implicit val interpreters = EGI()
  import interpreters._

  lazy val proxyCache = TimeCache { () ⇒
    val proxy = EGIAuthentication.proxy(authentication, voName, vomsURLs, fqan).get
    (proxy, preference(EGIEnvironment.ProxyRenewalTime))
  }

  lazy val tokenCache = TimeCache { () ⇒
    val token = EGIAuthentication.getToken(authentication, voName)
    (token, token.lifetime - preference(EGIEnvironment.TokenRenewalMarginTime))
  }

  override def start() = {
    Updater.delay(eagerSubmissionAgent)
    super.start()
  }

  override def stop() =
    try super.stop()
    finally {
      eagerSubmissionAgent.stop = true
      def usageControls = storages().map(_._2.usageControl) ++ List(batchJobService.usageControl)
      JobManager ! StopEnvironment(this, usageControls)
    }

  implicit def webdavlocationIsStorage = new StorageInterface[WebDavLocation] {
    def webdavServer(location: WebDavLocation) = gridscale.webdav.WebDAVSServer(location.url, proxyCache().factory)

    override def home(t: WebDavLocation): String = ""
    override def child(t: WebDavLocation, parent: String, child: String): String = gridscale.RemotePath.child(parent, child)
    override def parent(t: WebDavLocation, path: String): Option[String] = gridscale.RemotePath.parent(path)
    override def name(t: WebDavLocation, path: String): String = gridscale.RemotePath.name(path)

    override def exists(t: WebDavLocation, path: String): Boolean = gridscale.webdav.exists(webdavServer(t), path)
    override def list(t: WebDavLocation, path: String): Seq[gridscale.ListEntry] = gridscale.webdav.list(webdavServer(t), path)
    override def makeDir(t: WebDavLocation, path: String): Unit = gridscale.webdav.mkDirectory(webdavServer(t), path)
    override def rmDir(t: WebDavLocation, path: String): Unit = gridscale.webdav.rmDirectory(webdavServer(t), path)
    override def rmFile(t: WebDavLocation, path: String): Unit = gridscale.webdav.rmFile(webdavServer(t), path)
    override def mv(t: WebDavLocation, from: String, to: String): Unit = gridscale.webdav.mv(webdavServer(t), from, to)

    override def upload(t: WebDavLocation, src: File, dest: String, options: storage.TransferOptions): Unit =
      StorageInterface.upload(true, gridscale.webdav.writeStream(webdavServer(t), _, _))(src, dest, options)
    override def download(t: WebDavLocation, src: String, dest: File, options: storage.TransferOptions): Unit =
      StorageInterface.download(true, gridscale.webdav.readStream[Unit](webdavServer(t), _, _))(src, dest, options)

  }

  def bdiis: Seq[gridscale.egi.BDIIServer] =
    bdiiURL.map(b ⇒ Seq(EGIEnvironment.toBDII(new java.net.URI(b)))).getOrElse(EGIEnvironment.defaultBDIIs)

  val storages = Cache {
    val webdavStorages = findWorking(bdiis, (b: BDIIServer) ⇒ webDAVs(b, voName))
    if (!webdavStorages.isEmpty) webdavStorages.map { location ⇒
      def isConnectionError(t: Throwable) = t match {
        case _: _root_.gridscale.authentication.AuthenticationException ⇒ true
        case _ ⇒ false
      }

      location -> StorageService(
        EGIEnvironment.WebDavLocation(location),
        root = "",
        id = location,
        environment = env,
        remoteStorage = CurlRemoteStorage(location, voName, debug, preference(EGIEnvironment.RemoteCopyTimeout)),
        concurrency = preference(EGIEnvironment.ConnexionsByWebDAVSE),
        isConnectionError = isConnectionError
      )
    }
    else throw new InternalProcessingError("No WebDAV storage available for the VO")
  }

  override def trySelectStorage(usedFiles: ⇒ Vector[File]) = {
    import EGIEnvironment._
    import org.openmole.tool.file._
    import org.openmole.core.tools.math._

    val sss = storages().map(_._2)
    if (sss.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")

    // val nonEmpty = sss.filter(!_.usageControl.isEmpty)

    case class FileInfo(size: Long, hash: String)

    val usedFilesInfo = usedFiles.map { f ⇒ f → FileInfo(f.size, fileService.hash(f).toString) }.toMap
    val totalFileSize = usedFilesInfo.values.toSeq.map(_.size).sum
    val onStorage = replicaCatalog.forHashes(usedFilesInfo.values.toVector.map(_.hash), sss.map(_.id)).groupBy(_.storage)

    def minOption(v: Seq[Double]) = if (v.isEmpty) None else Some(v.min)
    def maxOption(v: Seq[Double]) = if (v.isEmpty) None else Some(v.max)

    val times = sss.flatMap(_.quality.time)
    val maxTime = maxOption(times)
    val minTime = minOption(times)

    //        val availablities = nonEmpty.flatMap(_.usageControl.availability)
    //        val maxAvailability = maxOption(availablities)
    //        val minAvailability = minOption(availablities)

    def rate(ss: StorageService[_]) = {
      val sizesOnStorage = usedFilesInfo.filter { case (_, info) ⇒ onStorage.getOrElse(ss.id, Set.empty).exists(_.hash == info.hash) }.values.map { _.size }
      val sizeOnStorage = sizesOnStorage.sum

      val sizeFactor = if (totalFileSize != 0) sizeOnStorage.toDouble / totalFileSize else 0.0

      val timeFactor =
        (minTime, maxTime, ss.quality.time) match {
          case (Some(minTime), Some(maxTime), Some(time)) if (maxTime > minTime) ⇒ 0.0 - time.normalize(minTime, maxTime)
          case _ ⇒ 0.0
        }

      //          val availabilityFactor =
      //            (minAvailability, maxAvailability, ss.usageControl.availability) match {
      //              case (Some(minAvailability), Some(maxAvailability), Some(availability)) if (maxAvailability > minAvailability) ⇒ 0.0 - availability.normalize(minAvailability, maxAvailability)
      //              case _ ⇒ 0.0
      //            }

      math.pow(
        preference(StorageSizeFactor) * sizeFactor +
          preference(StorageTimeFactor) * timeFactor +
          //              preference(StorageAvailabilityFactor) * availabilityFactor +
          preference(StorageSuccessRateFactor) * ss.quality.successRate.getOrElse(0.0),
        preference(StorageFitnessPower)
      )
    }

    val weighted = sss.map(s ⇒ math.max(rate(s), preference(EGIEnvironment.MinValueForSelectionExploration)) -> s)

    val storage = org.openmole.tool.random.multinomialDraw(weighted)(randomProvider())
    val token = UsageControl.tryGetToken(storage.usageControl)
    token.map(t ⇒ storage -> t)
  }

  import gridscale.dirac._

  lazy val diracService = {
    def userDiracService =
      for {
        s ← service
        g ← group
      } yield gridscale.dirac.Service(s, g)

    val diracService = userDiracService getOrElse getService(voName)
    val s = server(diracService, implicitly[EGIAuthenticationInterface[A]].apply(authentication), EGIAuthentication.CACertificatesDir)
    delegate(s, implicitly[EGIAuthenticationInterface[A]].apply(authentication), tokenCache())
    s
  }

  lazy val diracJobGroup = java.util.UUID.randomUUID().toString.filter(_ != '-')
  lazy val jobService = BatchJobService(env, concurrency = preference(EGIEnvironment.ConnexionsByWebDAVSE))

  def submit(serializedJob: SerializedJob) = {
    import org.openmole.tool.file._

    def storageLocations = storages().map(id ⇒ id._1 -> id._1).toMap

    def jobScript =
      JobScript(
        voName = voName,
        memory = BatchEnvironment.openMOLEMemoryValue(openMOLEMemory).toMegabytes.toInt,
        threads = 1,
        debug = debug,
        storageLocations
      )

    val outputFilePath = serializedJob.storage.child(serializedJob.path, uniqName("job", ".out"))

    newFile.withTmpFile("script", ".sh") { script ⇒
      script.content = jobScript(serializedJob, outputFilePath)

      val jobDescription =
        JobDescription(
          executable = "/bin/bash",
          arguments = s"-x ${script.getName}",
          inputSandbox = Seq(script),
          stdOut = Some(EGIEnvironment.stdOutFileName),
          stdErr = Some(EGIEnvironment.stdErrFileName),
          outputSandbox = Seq(EGIEnvironment.stdOutFileName, EGIEnvironment.stdErrFileName),
          cpuTime = cpuTime
        )

      val jid = gridscale.dirac.submit(diracService, jobDescription, tokenCache(), Some(diracJobGroup))
      org.openmole.plugin.environment.batch.jobservice.BatchJob(jid, outputFilePath)
    }
  }

  val jobStateCache = TimeCache { () ⇒
    val states = gridscale.dirac.queryGroupState(diracService, tokenCache(), diracJobGroup)
    states.toMap -> preference(EGIEnvironment.JobGroupRefreshInterval)
  }

  def state(id: gridscale.dirac.JobID) = {
    val state = jobStateCache().getOrElse(id.id, throw new InternalProcessingError(s"Job ${id.id} not found in group ${diracJobGroup} of DIRAC server."))
    org.openmole.plugin.environment.gridscale.GridScaleJobService.translateStatus(state)
  }

  def delete(id: gridscale.dirac.JobID) =
    gridscale.dirac.delete(diracService, tokenCache(), id) //clean(LocalHost(), id)

  def stdOutErr(id: gridscale.dirac.JobID) = newFile.withTmpDir { tmpDir ⇒
    import org.openmole.tool.file._
    tmpDir.mkdirs()
    gridscale.dirac.downloadOutputSandbox(diracService, tokenCache(), id, tmpDir)

    def stdOut =
      if ((tmpDir / EGIEnvironment.stdOutFileName) exists) (tmpDir / EGIEnvironment.stdOutFileName).content
      else ""

    def stdErr =
      if ((tmpDir / EGIEnvironment.stdErrFileName) exists) (tmpDir / EGIEnvironment.stdErrFileName).content
      else ""

    (stdOut, stdErr)
  }

  lazy val batchJobService = BatchJobService(env, preference(EGIEnvironment.ConnectionsToDIRAC))

  override def trySelectJobService() =
    UsageControl.tryGetToken(batchJobService.usageControl).map(token ⇒ (batchJobService, token))

}