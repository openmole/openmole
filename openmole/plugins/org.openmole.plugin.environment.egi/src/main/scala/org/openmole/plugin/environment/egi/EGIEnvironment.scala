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

import org.bouncycastle.voms.VOMSAttribute
import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.communication.storage
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.threadprovider.Updater
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.jobservice.{ BatchJobService, JobServiceInterface }
import org.openmole.plugin.environment.batch.refresh.JobManager
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.crypto.Cypher
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job.Job
import org.openmole.plugin.environment.batch.environment
import squants._
import squants.information._

import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ HashMap, MultiMap, Set }
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
import org.openmole.tool.logger.JavaLogger
//import org.openmole.tool.random._
//import squants.information.Information
import squants.time.Time
import squants.time.TimeConversions._
import org.openmole.tool.cache._
import gridscale.egi._
import effectaside._

object EGIEnvironment extends JavaLogger {

  import util._

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

  implicit def webdavlocationIsStorage(implicit httpEffect: Effect[_root_.gridscale.http.HTTP]) = new StorageInterface[WebDavStorage] with EnvironmentStorage[WebDavStorage] with HierarchicalStorageInterface[WebDavStorage] {
    def webdavServer(location: WebDavStorage) = gridscale.webdav.WebDAVSServer(location.url, location.proxyCache().factory)

    override def accessControl(t: WebDavStorage): AccessControl = t.accessControl

    override def child(t: WebDavStorage, parent: String, child: String): String = gridscale.RemotePath.child(parent, child)
    override def parent(t: WebDavStorage, path: String): Option[String] = gridscale.RemotePath.parent(path)
    override def name(t: WebDavStorage, path: String): String = gridscale.RemotePath.name(path)

    override def exists(t: WebDavStorage, path: String): Boolean = t.qualityControl { gridscale.webdav.exists(webdavServer(t), path) }
    override def list(t: WebDavStorage, path: String): Seq[gridscale.ListEntry] = t.qualityControl { gridscale.webdav.list(webdavServer(t), path) }
    override def makeDir(t: WebDavStorage, path: String): Unit = t.qualityControl { gridscale.webdav.mkDirectory(webdavServer(t), path) }
    override def rmDir(t: WebDavStorage, path: String): Unit = t.qualityControl { gridscale.webdav.rmDirectory(webdavServer(t), path) }
    override def rmFile(t: WebDavStorage, path: String): Unit = t.qualityControl { gridscale.webdav.rmFile(webdavServer(t), path) }

    override def upload(t: WebDavStorage, src: File, dest: String, options: storage.TransferOptions): Unit = t.qualityControl {
      StorageInterface.upload(true, gridscale.webdav.writeStream(webdavServer(t), _, _))(src, dest, options)
      //if (!exists(t, dest)) throw new InternalProcessingError(s"File $src has been successfully uploaded to $dest on $t but does not exist.")
    }

    override def download(t: WebDavStorage, src: String, dest: File, options: storage.TransferOptions): Unit = t.qualityControl {
      StorageInterface.download(true, gridscale.webdav.readStream[Unit](webdavServer(t), _, _))(src, dest, options)
    }

    override def id(s: WebDavStorage): String = s.url
    override def environment(s: WebDavStorage): BatchEnvironment = s.environment
  }

  case class WebDavStorage(url: String, accessControl: AccessControl, qualityControl: QualityControl, proxyCache: TimeCache[VOMS.VOMSCredential], environment: EGIEnvironment[_])

  def stdOutFileName = "output"
  def stdErrFileName = "error"

  implicit def isJobService[A]: JobServiceInterface[EGIEnvironment[A]] = new JobServiceInterface[EGIEnvironment[A]] {
    override type J = gridscale.dirac.JobID
    override def submit(env: EGIEnvironment[A], serializedJob: SerializedJob): J = env.submit(serializedJob)
    override def state(env: EGIEnvironment[A], j: J) = env.state(j)
    override def delete(env: EGIEnvironment[A], j: J): Unit = env.delete(j)
    override def stdOutErr(env: EGIEnvironment[A], j: J) = env.stdOutErr(j)

    override def accessControl(js: EGIEnvironment[A]): AccessControl = js.diracaccessControl
  }

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

  import EGIEnvironment._
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
    BatchEnvironment.start(this)
  }

  override def stop() = {
    def accessControls = storages().map(_._2.accessControl) ++ List(diracaccessControl)
    BatchEnvironment.clean(this, accessControls)
  }

  def bdiis: Seq[gridscale.egi.BDIIServer] =
    bdiiURL.map(b ⇒ Seq(EGIEnvironment.toBDII(new java.net.URI(b)))).getOrElse(EGIEnvironment.defaultBDIIs)

  lazy val storages = Cache {
    val webdavStorages = findFirstWorking(bdiis) { b: BDIIServer ⇒ webDAVs(b, voName) }
    if (!webdavStorages.isEmpty) webdavStorages.map { location ⇒
      def isConnectionError(t: Throwable) = t match {
        case _: _root_.gridscale.authentication.AuthenticationException ⇒ true
        case _ ⇒ false
      }

      val storage = WebDavStorage(location, AccessControl(preference(EGIEnvironment.ConnexionsByWebDAVSE)), QualityControl(preference(BatchEnvironment.QualityHysteresis)), proxyCache, env)
      def storageSpace = StorageSpace.hierarchicalStorageSpace(storage, "", location, isConnectionError)
      (Lazy(storageSpace), storage)
    }
    else throw new InternalProcessingError("No WebDAV storage available for the VO")
  }

  override def serializeJob(batchExecutionJob: BatchExecutionJob) = {
    import EGIEnvironment._
    import org.openmole.tool.file._
    import org.openmole.core.tools.math._

    def selectStorage = {
      val sss = storages()
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
    val remoteStorage = CurlRemoteStorage(storageService.url, voName, debug, preference(EGIEnvironment.RemoteCopyTimeout))

    BatchEnvironment.serializeJob(
      storageService,
      remoteStorage,
      batchExecutionJob,
      StorageSpace.createJobDirectory(storageService, storageSpace()), storageSpace().replicaDirectory)

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

  def submit(serializedJob: SerializedJob) = {
    import org.openmole.tool.file._

    def storageLocations = storages().map(_._2).map(s ⇒ implicitly[EnvironmentStorage[WebDavStorage]].id(s) -> s.url).toMap

    def jobScript =
      JobScript(
        voName = voName,
        memory = BatchEnvironment.openMOLEMemoryValue(openMOLEMemory).toMegabytes.toInt,
        threads = 1,
        debug = debug,
        storageLocations
      )

    newFile.withTmpFile("script", ".sh") { script ⇒
      script.content = jobScript(serializedJob)

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

      gridscale.dirac.submit(diracService, jobDescription, tokenCache(), Some(diracJobGroup))
    }
  }

  override def finishedJob(job: ExecutionJob): Unit = EGIEnvironment.eagerSubmit(this)

  lazy val jobStateCache = TimeCache { () ⇒
    val states = gridscale.dirac.queryState(diracService, tokenCache(), groupId = Some(diracJobGroup))
    states.toMap -> preference(EGIEnvironment.JobGroupRefreshInterval)
  }

  def state(id: gridscale.dirac.JobID) = {
    val state = jobStateCache().getOrElse(id.id, throw new InternalProcessingError(s"Job ${id.id} not found in group ${diracJobGroup} of DIRAC server."))
    org.openmole.plugin.environment.gridscale.GridScaleJobService.translateStatus(state)
  }

  def delete(id: gridscale.dirac.JobID) = {
    gridscale.dirac.delete(diracService, tokenCache(), id) //clean(LocalHost(), id)
  }

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

  lazy val diracaccessControl = environment.AccessControl(preference(EGIEnvironment.ConnectionsToDIRAC))

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(env, serializedJob)

  override def updateInterval =
    UpdateInterval.fixed(preference(EGIEnvironment.JobGroupRefreshInterval))

}
