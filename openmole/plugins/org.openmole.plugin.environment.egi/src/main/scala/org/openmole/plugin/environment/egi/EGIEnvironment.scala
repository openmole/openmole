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

import org.openmole.core.exception.{ InternalProcessingError, MultipleException }
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job.Job
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.batch.environment.{ BatchJobControl, _ }
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.crypto.Cypher
import squants.information._

import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ HashMap, MultiMap, Set }
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.workflow.dsl._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.exception._
import gridscale.egi._
import org.openmole.tool.cache._
import squants.time.Time
import squants.time.TimeConversions._

object EGIEnvironment extends JavaLogger {

  val FetchResourcesTimeOut = ConfigurationLocation("EGIEnvironment", "FetchResourcesTimeOut", Some(1 minutes))
  val CACertificatesSite = ConfigurationLocation("EGIEnvironment", "CACertificatesSite", Some("http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/"))
  val CACertificatesCacheTime = ConfigurationLocation("EGIEnvironment", "CACertificatesCacheTime", Some(7 days))
  val CACertificatesDownloadTimeOut = ConfigurationLocation("EGIEnvironment", "CACertificatesDownloadTimeOut", Some(2 minutes))
  val VOInformationSite = ConfigurationLocation("EGIEnvironment", "VOInformationSite", Some("http://operations-portal.egi.eu/xml/voIDCard/public/all/true"))
  val VOCardDownloadTimeOut = ConfigurationLocation("EGIEnvironment", "VOCardDownloadTimeOut", Some(2 minutes))
  val VOCardCacheTime = ConfigurationLocation("EGIEnvironment", "VOCardCacheTime", Some(6 hours))

  val EagerSubmissionMinNumberOfJobs = ConfigurationLocation("EGIEnvironment", "EagerSubmissionMinNumberOfJobs", Some(100))
  val EagerSubmissionNumberOfJobs = ConfigurationLocation("EGIEnvironment", "EagerSubmissionNumberOfJobs", Some(3))

  //  val ConnectionsBySRMSE = ConfigurationLocation("EGIEnvironment", "ConnectionsSRMSE", Some(10))
  val ConnexionsByWebDAVSE = ConfigurationLocation("EGIEnvironment", "ConnectionsByWebDAVSE", Some(100))
  val ConnectionsToDIRAC = ConfigurationLocation("EGIEnvironment", "ConnectionsToDIRAC", Some(10))
  //
  val ProxyLifeTime = ConfigurationLocation("EGIEnvironment", "ProxyTime", Some(24 hours))
  //  val MyProxyTime = ConfigurationLocation("EGIEnvironment", "MyProxyTime", Some(7 days))
  val ProxyRenewalTime = ConfigurationLocation("EGIEnvironment", "ProxyRenewalTime", Some(1 hours))
  val TokenRenewalTime = ConfigurationLocation("EGIEnvironment", "TokenRenewalTime", Some(1 hours))
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

  def defaultBDIIs(implicit preference: Preference) =
    preference(EGIEnvironment.DefaultBDIIs).map(b ⇒ new java.net.URI(b)).map(toBDII)

  def stdOutFileName = "output"
  def stdErrFileName = "error"

  def eagerSubmit(environment: EGIEnvironment[_])(implicit preference: Preference) = {
    val jobs = environment.jobs
    val jobSize = jobs.size

    val minOversub = preference(EGIEnvironment.EagerSubmissionMinNumberOfJobs)
    val numberOfSimultaneousExecutionForAJob = preference(EGIEnvironment.EagerSubmissionNumberOfJobs)

    var nbRessub = if (jobSize < minOversub) minOversub - jobSize else 0
    lazy val executionJobs = jobs.groupBy(_.job)

    if (nbRessub > 0) {
      // Resubmit nbRessub jobs in a fair manner
      val order = new HashMap[Int, Set[Job]] with MultiMap[Int, Job]
      var keys = new TreeSet[Int]

      for (job ← executionJobs.keys) {
        val nb = executionJobs(job).size
        if (nb < numberOfSimultaneousExecutionForAJob) {
          order.addBinding(nb, job)
          keys += nb
        }
      }

      if (!keys.isEmpty) {
        while (nbRessub > 0 && keys.head < numberOfSimultaneousExecutionForAJob) {
          val key = keys.head
          val jobs = order(keys.head)
          val job =
            jobs.find(j ⇒ executionJobs(j).isEmpty) match {
              case Some(j) ⇒ j
              case None ⇒
                jobs.find(j ⇒ !executionJobs(j).exists(_.state != ExecutionState.SUBMITTED)) match {
                  case Some(j) ⇒ j
                  case None    ⇒ jobs.head
                }
            }

          environment.submit(job)

          order.removeBinding(key, job)
          if (jobs.isEmpty) keys -= key

          order.addBinding(key + 1, job)
          keys += (key + 1)
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
  )(implicit authentication: EGIAuthentication, services: BatchEnvironment.Services, cypher: Cypher, workspace: Workspace, varName: sourcecode.Name) = {

    EnvironmentProvider { () ⇒
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
    if (storages.map(_.toOption).flatten.isEmpty) throw new InternalProcessingError(s"No webdav storage is working for the VO $voName", MultipleException(storages.collect { case util.Failure(e) ⇒ e }))
    jobService
  }

  override def stop() = {
    storages.map(_.toOption).flatten.foreach { case (space, storage) ⇒ HierarchicalStorageSpace.clean(storage, space) }
  }

  def bdiis: Seq[gridscale.egi.BDIIServer] =
    bdiiURL.map(b ⇒ Seq(EGIEnvironment.toBDII(new java.net.URI(b)))).getOrElse(EGIEnvironment.defaultBDIIs)

  lazy val storages = {
    val webdavStorages = findFirstWorking(bdiis) { b: BDIIServer ⇒ webDAVs(b, voName) }
    if (!webdavStorages.isEmpty) webdavStorages.map { location ⇒
      def isConnectionError(t: Throwable) = t match {
        case _: _root_.gridscale.authentication.AuthenticationException ⇒ true
        case _ ⇒ false
      }

      val storage = WebDavStorage(location, AccessControl(preference(EGIEnvironment.ConnexionsByWebDAVSE)), QualityControl(preference(BatchEnvironment.QualityHysteresis)), proxyCache, env)
      def storageSpace = util.Try { HierarchicalStorageSpace.create(storage, "", location, isConnectionError) }
      storageSpace.map(s ⇒ (s, storage))
    }
    else throw new InternalProcessingError(s"No WebDAV storage available for the VO $voName")
  }

  def execute(batchExecutionJob: BatchExecutionJob) = {
    import EGIEnvironment._
    import org.openmole.core.tools.math._
    import org.openmole.tool.file._

    def selectStorage = {
      val sss = storages.map(_.toOption).flatten
      if (sss.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")

      if (sss.size == 1) sss.head
      else {
        // val nonEmpty = sss.filter(!_.accessControl.isEmpty)

        case class FileInfo(size: Long, hash: String)

        def fileSize(file: File) = (if (file.isDirectory) fileService.archiveForDir(file).file else file).size

        val usedFiles = BatchEnvironment.jobFiles(batchExecutionJob)
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

    val (storageSpace, storageService) = selectStorage

    val jobDirectory = HierarchicalStorageSpace.createJobDirectory(storageService, storageSpace)

    tryOnError { StorageService.rmDirectory(storageService, jobDirectory) } {
      val remoteStorage = CurlRemoteStorage(storageService.url, jobDirectory, voName, debug, preference(EGIEnvironment.RemoteCopyTimeout))

      val sj = BatchEnvironment.serializeJob(
        storageService,
        remoteStorage,
        batchExecutionJob,
        jobDirectory,
        storageSpace.replicaDirectory)

      val outputPath = StorageService.child(storageService, jobDirectory, uniqName("job", ".out"))
      val job = jobService.submit(sj, outputPath, storageService.url)

      BatchJobControl(
        env,
        StorageService.id(storageService),
        () ⇒ jobService.state(job),
        () ⇒ jobService.delete(job),
        () ⇒ jobService.stdOutErr(job),
        () ⇒ outputPath,
        StorageService.download(storageService, _, _, _),
        () ⇒ StorageService.rmDirectory(storageService, jobDirectory)
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

    val diracService = userDiracService getOrElse getService(voName)
    val s = server(diracService, implicitly[EGIAuthenticationInterface[A]].apply(authentication), EGIAuthentication.CACertificatesDir)
    delegate(s, implicitly[EGIAuthenticationInterface[A]].apply(authentication), tokenCache())
    EGIJobService(s, env)
  }

  override def updateInterval =
    UpdateInterval.fixed(preference(EGIEnvironment.JobGroupRefreshInterval))

  override def finishedJob(job: ExecutionJob): Unit = EGIEnvironment.eagerSubmit(env)

}
