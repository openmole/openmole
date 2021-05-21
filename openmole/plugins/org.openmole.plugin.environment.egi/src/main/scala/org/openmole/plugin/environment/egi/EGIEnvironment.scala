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

import gridscale.egi._
import org.openmole.core.communication.storage.TransferOptions
import org.openmole.core.exception.{ InternalProcessingError, MultipleException }
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.preference.{ Preference, PreferenceLocation }
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.batch.environment.{ BatchJobControl, _ }
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.cache._
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.exception._
import org.openmole.tool.logger.JavaLogger
import squants.information._
import squants.time.Time
import squants.time.TimeConversions._

import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ HashMap, MultiMap, Set }
import scala.concurrent.{ Await, Future }

object EGIEnvironment extends JavaLogger {

  val FetchResourcesTimeOut = PreferenceLocation("EGIEnvironment", "FetchResourcesTimeOut", Some(1 minutes))
  val CACertificatesSite = PreferenceLocation("EGIEnvironment", "CACertificatesSite", Some("https://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/"))
  val CACertificatesCacheTime = PreferenceLocation("EGIEnvironment", "CACertificatesCacheTime", Some(7 days))
  val CACertificatesDownloadTimeOut = PreferenceLocation("EGIEnvironment", "CACertificatesDownloadTimeOut", Some(2 minutes))
  val VOInformationSite = PreferenceLocation("EGIEnvironment", "VOInformationSite", Some("https://operations-portal.egi.eu/xml/voIDCard/public/all/true"))
  val VOCardDownloadTimeOut = PreferenceLocation("EGIEnvironment", "VOCardDownloadTimeOut", Some(2 minutes))
  val VOCardCacheTime = PreferenceLocation("EGIEnvironment", "VOCardCacheTime", Some(6 hours))

  val EagerSubmissionMinNumberOfJobs = PreferenceLocation("EGIEnvironment", "EagerSubmissionMinNumberOfJobs", Some(100))
  val EagerSubmissionNumberOfJobs = PreferenceLocation("EGIEnvironment", "EagerSubmissionNumberOfJobs", Some(3))

  //  val ConnectionsBySRMSE = ConfigurationLocation("EGIEnvironment", "ConnectionsSRMSE", Some(10))
  val ConnexionsByWebDAVSE = PreferenceLocation("EGIEnvironment", "ConnectionsByWebDAVSE", Some(100))
  val ConnectionsToDIRAC = PreferenceLocation("EGIEnvironment", "ConnectionsToDIRAC", Some(10))
  //
  val ProxyLifeTime = PreferenceLocation("EGIEnvironment", "ProxyTime", Some(24 hours))
  //  val MyProxyTime = ConfigurationLocation("EGIEnvironment", "MyProxyTime", Some(7 days))
  val ProxyRenewalTime = PreferenceLocation("EGIEnvironment", "ProxyRenewalTime", Some(1 hours))
  val TokenRenewalTime = PreferenceLocation("EGIEnvironment", "TokenRenewalTime", Some(1 hours))
  val JobGroupRefreshInterval = PreferenceLocation("EGIEnvironment", "JobGroupRefreshInterval", Some(1 minutes))
  //  val JobShakingHalfLife = ConfigurationLocation("EGIEnvironment", "JobShakingHalfLife", Some(30 minutes))
  //  val JobShakingMaxReady = ConfigurationLocation("EGIEnvironment", "JobShakingMaxReady", Some(100))
  //
  val RemoteCopyTimeout = PreferenceLocation("EGIEnvironment", "RemoteCopyTimeout", Some(30 minutes))
  //  val QualityHysteresis = ConfigurationLocation("EGIEnvironment", "QualityHysteresis", Some(100))
  val MinValueForSelectionExploration = PreferenceLocation("EGIEnvironment", "MinValueForSelectionExploration", Some(0.001))
  //  val ShallowWMSRetryCount = ConfigurationLocation("EGIEnvironment", "ShallowWMSRetryCount", Some(5))
  //
  //  val JobServiceFitnessPower = ConfigurationLocation("EGIEnvironment", "JobServiceFitnessPower", Some(2.0))

  val StorageFitnessPower = PreferenceLocation("EGIEnvironment", "StorageFitnessPower", Some(2.0))
  //
  val StorageSizeFactor = PreferenceLocation("EGIEnvironment", "StorageSizeFactor", Some(5.0))
  val StorageTimeFactor = PreferenceLocation("EGIEnvironment", "StorageTimeFactor", Some(1.0))
  //    val StorageAvailabilityFactor = ConfigurationLocation("EGIEnvironment", "StorageAvailabilityFactor", Some(10.0))
  val StorageSuccessRateFactor = PreferenceLocation("EGIEnvironment", "StorageSuccessRateFactor", Some(10.0))

  val DiracConnectionTimeout = PreferenceLocation("EGIEnvironment", "DiracConnectionTimeout", Some(1 minutes))
  val VOMSTimeout = PreferenceLocation("EGIEnvironment", "VOMSTimeout", Some(1 minutes))

  val ldapURLs =
    Seq(
      "topbdii.grif.fr",
      "bdii.ndgf.org",
      "lcg-bdii.cern.ch",
      "bdii-fzk.gridka.de",
      "topbdii.egi.cesga.es",
      "egee-bdii.cnaf.infn.it"
    ).map(h ⇒ s"ldap://$h:2170")

  val DefaultBDIIs = PreferenceLocation("EGIEnvironment", "DefaultBDIIs", Some(ldapURLs))

  def toBDII(bdii: java.net.URI)(implicit preference: Preference) = BDIIServer(bdii.getHost, bdii.getPort, preference(FetchResourcesTimeOut))

  def defaultBDIIs(implicit preference: Preference) =
    preference(EGIEnvironment.DefaultBDIIs).map(b ⇒ new java.net.URI(b)).map(toBDII)

  def stdOutFileName = "output"
  def stdErrFileName = "error"

  def eagerSubmit(environment: EGIEnvironment[_])(implicit preference: Preference, serializerService: SerializerService) = {
    val jobs = environment.jobs
    val jobSize = jobs.size

    val minOversub = preference(EGIEnvironment.EagerSubmissionMinNumberOfJobs)
    val numberOfSimultaneousExecutionForAJob = preference(EGIEnvironment.EagerSubmissionNumberOfJobs)

    var nbRessub = if (jobSize < minOversub) minOversub - jobSize else 0

    type ExecutionJobId = collection.immutable.Set[Long]
    lazy val executionJobsMap: Map[ExecutionJobId, List[BatchExecutionJob]] = jobs.groupBy(_.storedJob.storedMoleJobs.map(_.id).toSet)

    if (nbRessub > 0) {
      // Resubmit nbRessub jobs in a fair manner
      val jobKeyMap = new HashMap[Int, Set[ExecutionJobId]] with MultiMap[Int, ExecutionJobId]
      var nbRuns = new TreeSet[Int]

      for ((ids, jobs) ← executionJobsMap) {
        val size = jobs.size
        if (size < numberOfSimultaneousExecutionForAJob) {
          jobKeyMap.addBinding(size, ids)
          nbRuns += size
        }
      }

      if (!nbRuns.isEmpty) {
        while (nbRessub > 0 && nbRuns.head < numberOfSimultaneousExecutionForAJob) {
          val size = nbRuns.head
          val jobKey = jobKeyMap(nbRuns.head)

          val job =
            jobKey.find(j ⇒ executionJobsMap(j).isEmpty) match {
              case Some(j) ⇒ j
              case None ⇒
                jobKey.find(j ⇒ !executionJobsMap(j).exists(j ⇒ j.state != ExecutionState.SUBMITTED)) match {
                  case Some(j) ⇒ j
                  case None    ⇒ jobKey.head
                }
            }

          environment.submit(JobStore.load(executionJobsMap(job).head.storedJob))

          jobKeyMap.removeBinding(size, job)
          if (jobKey.isEmpty) nbRuns -= size

          jobKeyMap.addBinding(size + 1, job)
          nbRuns += (size + 1)
          nbRessub -= 1
        }
      }
    }
  }

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
  )(implicit authentication: EGIAuthentication, cypher: Cypher, workspace: Workspace, replicaCatalog: ReplicaCatalog, varName: sourcecode.Name) = {

    EnvironmentProvider { ms ⇒
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
        authentication = authentication,
        services = BatchEnvironment.Services(ms)
      )
    }
  }

}

class EGIEnvironment[A: EGIAuthenticationInterface](
  val voName:            String,
  val service:           Option[String],
  val group:             Option[String],
  val bdiiURL:           Option[String],
  val vomsURLs:          Option[Seq[String]],
  val fqan:              Option[String],
  val cpuTime:           Option[Time],
  val openMOLEMemory:    Option[Information],
  val debug:             Boolean,
  val name:              Option[String],
  val authentication:    A,
  implicit val services: BatchEnvironment.Services
)(implicit workspace: Workspace) extends BatchEnvironment { env ⇒

  import services._

  implicit val interpreters = EGI()
  import interpreters._

  lazy val proxyCache = TimeCache { () ⇒
    val proxy = EGIAuthentication.proxy(authentication, voName, vomsURLs, fqan).get
    (proxy, preference(EGIEnvironment.ProxyRenewalTime))
  }

  lazy val tokenCache = TimeCache { () ⇒
    val token = EGIAuthentication.getToken(authentication, voName)
    (token, preference(EGIEnvironment.TokenRenewalTime))
  }

  override def start() = {
    proxyCache()
    if (storages().map(_.toOption).flatten.isEmpty) throw new InternalProcessingError(s"No webdav storage is working for the VO $voName", MultipleException(storages().collect { case util.Failure(e) ⇒ e }))
    jobService
  }

  override def stop() = {
    stopped = true
    storages().map(_.toOption).flatten.foreach { case (space, storage) ⇒ HierarchicalStorageSpace.clean(storage, space, background = false) }
    BatchEnvironment.waitJobKilled(this)
  }

  def bdiis: Seq[gridscale.egi.BDIIServer] =
    bdiiURL.map(b ⇒ Seq(EGIEnvironment.toBDII(new java.net.URI(b)))).getOrElse(EGIEnvironment.defaultBDIIs)

  val storages = Lazy {
    val webdavStorages = findFirstWorking(bdiis) { b: BDIIServer ⇒ webDAVs(b, voName) }

    if (!webdavStorages.isEmpty) {
      webdavStorages.map { location ⇒
        threadProvider.submit {
          def isConnectionError(t: Throwable) = {
            (t, t.getCause) match {
              case (_: _root_.gridscale.authentication.AuthenticationException, _) ⇒ true
              case (_, _: java.net.SocketException) ⇒ true
              case _ ⇒ false
            }
          }

          val storage = WebDavStorage(location, AccessControl(preference(EGIEnvironment.ConnexionsByWebDAVSE)), QualityControl(preference(BatchEnvironment.QualityHysteresis)), proxyCache, env)
          val storageSpace = util.Try { HierarchicalStorageSpace.create(storage, "", location, isConnectionError) }
          storageSpace.map { s ⇒ (s, storage) }
        }
      }.map(Await.result(_, scala.concurrent.duration.Duration.Inf))
    }
    else throw new InternalProcessingError(s"No WebDAV storage available for the VO $voName")

  }

  def execute(batchExecutionJob: BatchExecutionJob) = {
    import EGIEnvironment._
    import org.openmole.core.tools.math._
    import org.openmole.tool.file._

    def selectStorage = {
      val sss = storages().map(_.toOption).flatten
      if (sss.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")

      if (sss.size == 1) sss.head
      else {
        // val nonEmpty = sss.filter(!_.accessControl.isEmpty)

        case class FileInfo(size: Long, hash: String)

        def fileSize(file: File) = (if (file.isDirectory) fileService.archiveForDir(file) else file).size

        val usedFiles = BatchEnvironment.jobFiles(batchExecutionJob, env)
        val usedFilesInfo = usedFiles.map { f ⇒ f → FileInfo(fileSize(f), fileService.hash(f).toString) }.toMap
        val totalFileSize = usedFilesInfo.values.toSeq.map(_.size).sum

        val onStorage = replicaCatalog.forHashes(usedFilesInfo.values.toVector.map(_.hash), sss.map(_._2).map(implicitly[EnvironmentStorage[WebDavStorage]].id)).groupBy(_.storage)

        def minOption(v: Seq[Double]) = if (v.isEmpty) None else Some(v.min)

        def maxOption(v: Seq[Double]) = if (v.isEmpty) None else Some(v.max)

        val times = sss.flatMap(_._2.qualityControl.time)
        val maxTime = maxOption(times)
        val minTime = minOption(times)

        //        val availablities = nonEmpty.flatMap(_.accessControl.availability)
        //        val maxAvailability = maxOption(availablities)
        //        val minAvailability = minOption(availablities)

        def rate(ss: WebDavStorage) = {
          val sizesOnStorage = usedFilesInfo.filter { case (_, info) ⇒ onStorage.getOrElse(implicitly[EnvironmentStorage[WebDavStorage]].id(ss), Set.empty).exists(_.hash == info.hash) }.values.map {
            _.size
          }
          val sizeOnStorage = sizesOnStorage.sum

          val sizeFactor = if (totalFileSize != 0) sizeOnStorage.toDouble / totalFileSize else 0.0

          val timeFactor =
            (minTime, maxTime, ss.qualityControl.time) match {
              case (Some(minTime), Some(maxTime), Some(time)) if (maxTime > minTime) ⇒ 0.0 - time.normalize(minTime, maxTime)
              case _ ⇒ 0.0
            }

          //          val availabilityFactor =
          //            (minAvailability, maxAvailability, ss.accessControl.availability) match {
          //              case (Some(minAvailability), Some(maxAvailability), Some(availability)) if (maxAvailability > minAvailability) ⇒ 0.0 - availability.normalize(minAvailability, maxAvailability)
          //              case _ ⇒ 0.0
          //            }

          math.pow(
            preference(StorageSizeFactor) * sizeFactor +
              preference(StorageTimeFactor) * timeFactor +
              //              preference(StorageAvailabilityFactor) * availabilityFactor +
              preference(StorageSuccessRateFactor) * ss.qualityControl.successRate.getOrElse(0.0),
            preference(StorageFitnessPower)
          )
        }

        val weighted = sss.map(s ⇒ math.max(rate(s._2), preference(EGIEnvironment.MinValueForSelectionExploration)) -> s)
        val storage = org.openmole.tool.random.multinomialDraw(weighted)(randomProvider())
        storage
      }
    }

    val (space, storage) = selectStorage

    val jobDirectory = HierarchicalStorageSpace.createJobDirectory(storage, space)

    tryOnError { StorageService.rmDirectory(storage, jobDirectory) } {
      val remoteStorage = CurlRemoteStorage(storage.url, jobDirectory, voName, debug, preference(EGIEnvironment.RemoteCopyTimeout))

      def replicate(f: File, options: TransferOptions) =
        BatchEnvironment.toReplicatedFile(
          StorageService.uploadInDirectory(storage, _, space.replicaDirectory, _),
          StorageService.exists(storage, _),
          StorageService.rmFile(storage, _, background = true),
          env,
          StorageService.id(storage)
        )(f, options)

      def upload(f: File, options: TransferOptions) = StorageService.uploadInDirectory(storage, f, jobDirectory, options)

      val sj = BatchEnvironment.serializeJob(env, batchExecutionJob, remoteStorage, replicate, upload, StorageService.id(storage))
      val outputPath = StorageService.child(storage, jobDirectory, uniqName("job", ".out"))
      val job = jobService.submit(sj, outputPath, storage.url)

      BatchJobControl(
        () ⇒ UpdateInterval.fixed(preference(EGIEnvironment.JobGroupRefreshInterval)),
        () ⇒ StorageService.id(storage),
        () ⇒ jobService.state(job),
        () ⇒ jobService.delete(job),
        () ⇒ jobService.stdOutErr(job),
        () ⇒ outputPath,
        StorageService.download(storage, _, _, _),
        () ⇒ StorageService.rmDirectory(storage, jobDirectory)
      )
    }
  }

  import gridscale.dirac._

  lazy val jobService = {
    def userDiracService =
      for {
        s ← service
        g ← group
      } yield gridscale.dirac.Service(s, g)

    val diracService = userDiracService getOrElse getService(voName, EGIAuthentication.CACertificatesDir)
    val s = server(diracService, implicitly[EGIAuthenticationInterface[A]].apply(authentication), EGIAuthentication.CACertificatesDir)
    delegate(s, implicitly[EGIAuthenticationInterface[A]].apply(authentication), tokenCache())
    EGIJobService(s, env)
  }

  override def finishedJob(job: ExecutionJob): Unit = {
    if (!stopped) EGIEnvironment.eagerSubmit(env)
  }

}
