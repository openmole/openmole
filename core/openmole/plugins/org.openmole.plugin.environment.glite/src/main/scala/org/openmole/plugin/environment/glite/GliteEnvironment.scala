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

package org.openmole.plugin.environment.glite

import org.openmole.misc.filedeleter.FileDeleter
import org.openmole.misc.updater.Updater
import org.openmole.misc.workspace.ConfigurationLocation
import fr.iscpif.gridscale.information.BDII
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage._
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Logger
import org.openmole.core.model.job.IJob
import org.openmole.misc.exception._
import org.openmole.misc.tools.service.Duration._
import org.openmole.plugin.environment.gridscale._
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.control._
import org.openmole.misc.tools.service._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.batch.replication._
import org.openmole.misc.workspace._
import concurrent.stm._
import annotation.tailrec
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl
import ref.WeakReference

object GliteEnvironment extends Logger {

  val ProxyTime = new ConfigurationLocation("GliteEnvironment", "ProxyTime")
  val MyProxyTime = new ConfigurationLocation("GliteEnvironment", "MyProxyTime")

  val FetchRessourcesTimeOut = new ConfigurationLocation("GliteEnvironment", "FetchRessourcesTimeOut")
  val CACertificatesSite = new ConfigurationLocation("GliteEnvironment", "CACertificatesSite")

  val OverSubmissionInterval = new ConfigurationLocation("GliteEnvironment", "OverSubmissionInterval")
  val OverSubmissionMinNumberOfJob = new ConfigurationLocation("GliteEnvironment", "OverSubmissionMinNumberOfJob")
  val OverSubmissionNumberOfJobUnderMin = new ConfigurationLocation("GliteEnvironment", "OverSubmissionNumberOfJobUnderMin")
  val OverSubmissionNbSampling = new ConfigurationLocation("GliteEnvironment", "OverSubmissionNbSampling")
  val OverSubmissionSamplingWindowFactor = new ConfigurationLocation("GliteEnvironment", "OverSubmissionSamplingWindowFactor")

  val LocalThreadsBySE = new ConfigurationLocation("GliteEnvironment", "LocalThreadsBySE")
  val LocalThreadsByWMS = new ConfigurationLocation("GliteEnvironment", "LocalThreadsByWMS")
  val ProxyRenewalRatio = new ConfigurationLocation("GliteEnvironment", "ProxyRenewalRatio")
  val MinProxyRenewal = new ConfigurationLocation("GliteEnvironment", "MinProxyRenewal")
  val JobShakingHalfLife = new ConfigurationLocation("GliteEnvironment", "JobShakingHalfLife")
  val JobShakingMaxReady = new ConfigurationLocation("GliteEnvironment", "JobShakingMaxReady")

  val RemoteTimeout = new ConfigurationLocation("GliteEnvironment", "RemoteTimeout")
  val QualityHysteresis = new ConfigurationLocation("GliteEnvironment", "QualityHysteresis")
  val MinValueForSelectionExploration = new ConfigurationLocation("GliteEnvironment", "MinValueForSelectionExploration")
  val WMSRetryCount = new ConfigurationLocation("GliteEnvironment", "WMSRetryCount")

  Workspace += (ProxyTime, "PT24H")
  Workspace += (MyProxyTime, "P7D")

  Workspace += (FetchRessourcesTimeOut, "PT2M")
  Workspace += (CACertificatesSite, "http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/")

  Workspace += (LocalThreadsBySE, "10")
  Workspace += (LocalThreadsByWMS, "10")

  Workspace += (ProxyRenewalRatio, "0.2")

  Workspace += (MinProxyRenewal, "PT5M")

  Workspace += (OverSubmissionNbSampling, "10")
  Workspace += (OverSubmissionSamplingWindowFactor, "5")

  Workspace += (OverSubmissionInterval, "PT2M")

  Workspace += (OverSubmissionMinNumberOfJob, "100")
  Workspace += (OverSubmissionNumberOfJobUnderMin, "10")

  Workspace += (JobShakingHalfLife, "PT30M")
  Workspace += (JobShakingMaxReady, "100")

  Workspace += (RemoteTimeout, "PT5M")

  Workspace += (MinValueForSelectionExploration, "0.001")
  Workspace += (QualityHysteresis, "100")

  Workspace += (WMSRetryCount, "10")
}

class GliteEnvironment(
    val voName: String,
    val vomsURL: String,
    val bdii: String,
    val fqan: Option[String] = None,
    override val runtimeMemory: Option[Int] = None,
    val memory: Option[Int] = None,
    val cpuTime: Option[String] = None,
    val cpuNumber: Option[Int] = None,
    val jobType: Option[String] = None,
    val smpGranularity: Option[Int] = None,
    val myProxy: Option[MyProxy] = None,
    val architecture: Option[String] = None) extends BatchEnvironment with MemoryRequirement { env ⇒

  import GliteEnvironment._

  @transient lazy val id = voName + "@" + vomsURL
  @transient lazy val threadsBySE = Workspace.preferenceAsInt(LocalThreadsBySE)
  @transient lazy val threadsByWMS = Workspace.preferenceAsInt(LocalThreadsByWMS)

  type JS = GliteJobService
  type SS = GliteStorageService

  @transient lazy val registerAgents: Unit = {
    Updater.registerForUpdate(new OverSubmissionAgent(WeakReference(this)))
    Updater.registerForUpdate(new ProxyChecker(WeakReference(this)))
  }

  override def submit(job: IJob) = {
    registerAgents
    super.submit(job)
  }

  private def generateProxy = Workspace.persistentList(classOf[GliteAuthentication]).headOption match {
    case Some(a) ⇒
      val file = Workspace.newFile("proxy", ".x509")
      FileDeleter.deleteWhenGarbageCollected(file)
      val proxy = a._2.apply(
        vomsURL,
        voName,
        file,
        Workspace.preferenceAsDurationInS(ProxyTime),
        fqan)
      myProxy.foreach(mp ⇒ mp.delegate(proxy, mp.time.toSeconds))
      (proxy, file)
    case None ⇒ throw new UserBadDataError("No athentication has been initialized for glite.")
  }

  @transient @volatile var _authentication = generateProxy

  def authentication = synchronized {
    if (_authentication == null) _authentication = authenticate
    _authentication
  }

  def authenticate = synchronized {
    _authentication = generateProxy
    jobServices.foreach { _.delegated = false }
    _authentication
  }

  override def allJobServices = {
    val jss = getBDII.queryWMS(voName, Workspace.preferenceAsDurationInS(FetchRessourcesTimeOut).toInt)
    jss.map {
      js ⇒
        new GliteJobService {
          val jobService = js
          val environment = env
          val nbTokens = threadsByWMS
        }
    }
  }

  override def allStorages = {
    val stors = getBDII.querySRM(voName, Workspace.preferenceAsDurationInS(GliteEnvironment.FetchRessourcesTimeOut).toInt)
    stors.map {
      s ⇒ GliteStorageService(s, env, GliteAuthentication.CACertificatesDir)
    }
  }

  protected def normalizedFitness[T, S](fitness: ⇒ Iterable[(T, S, Double)]): Iterable[(T, S, Double)] = {
    def orMinForExploration(v: Double) = {
      val min = Workspace.preferenceAsDouble(GliteEnvironment.MinValueForSelectionExploration)
      if (v < min) min else v
    }
    val fit = fitness
    val maxFit = fit.map(_._3).max
    fit.map { case (c, t, f) ⇒ (c, t, orMinForExploration(f / maxFit)) }
  }

  override def selectAJobService =
    if (jobServices.size == 1) super.selectAJobService
    else {
      val maxTime = jobServices.map(_.time).max

      def fitness =
        jobServices.flatMap {
          cur ⇒
            cur.tryGetToken match {
              case None ⇒ None
              case Some(token) ⇒
                val time = cur.time
                val timeFactor =
                  if (time.isNaN || maxTime.isNaN || maxTime == 0.0) 0.0
                  else 1 - (time / maxTime)

                val jobFactor =
                  if (cur.submitted > 0 && cur.totalSubmitted > 0) (cur.runnig.toDouble / cur.submitted) * (cur.totalDone / cur.totalSubmitted)
                  else 0.0

                val availabilty = (cur.available.toDouble + 1) / cur.nbTokens
                val fitness = jobFactor + cur.successRate + timeFactor + availabilty
                Some((cur, token, fitness))
            }
        }

      @tailrec def selected(value: Double, jobServices: List[(JobService, AccessToken, Double)]): Option[(JobService, AccessToken)] =
        jobServices.headOption match {
          case Some((js, token, fitness)) ⇒
            if (value <= fitness) Some((js, token))
            else selected(value - fitness, jobServices.tail)
          case None ⇒ None
        }

      atomic { implicit txn ⇒
        val notLoaded = normalizedFitness(fitness)
        val totalFitness = notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum

        selected(Random.default.nextDouble * totalFitness, notLoaded.toList) match {
          case Some((jobService, token)) ⇒
            for {
              (s, t, _) ← notLoaded
              if (s.id != jobService.id)
            } s.releaseToken(t)
            jobService -> token
          case None ⇒
            for { (s, t, _) ← notLoaded } s.releaseToken(t)
            retry
        }
      }
    }

  override def selectAStorage(usedFileHashes: Iterable[(File, Hash)]) =
    if (storages.size == 1) super.selectAStorage(usedFileHashes)
    else {
      val totalFileSize = usedFileHashes.map { case (f, _) ⇒ f.size }.sum
      val onStorage = ReplicaCatalog.withClient(ReplicaCatalog.inCatalog(env.id)(_))
      val maxTime = jobServices.map(_.time).max

      def fitness(implicit tx: InTxn) =
        storages.flatMap {
          cur ⇒
            cur.tryGetToken match {
              case None ⇒ None
              case Some(token) ⇒
                val sizeOnStorage = usedFileHashes.filter { case (_, h) ⇒ onStorage.getOrElse(h.toString, Set.empty).contains(cur.id) }.map { case (f, _) ⇒ f.size }.sum
                val sizeFactor =
                  if (totalFileSize != 0) (sizeOnStorage.toDouble / totalFileSize) else 0.0

                val time = cur.time
                val timeFactor =
                  if (time.isNaN || maxTime.isNaN || maxTime == 0.0) 0.0
                  else 1 - (time / maxTime)

                val availabilty = (cur.available.toDouble + 1) / cur.nbTokens

                val fitness = cur.successRate + sizeFactor + timeFactor + availabilty
                Some((cur, token, fitness))
            }
        }

      @tailrec def selected(value: Double, storages: List[(StorageService, AccessToken, Double)]): Option[(StorageService, AccessToken)] = {
        storages.headOption match {
          case Some((storage, token, fitness)) ⇒
            if (value <= fitness) Some((storage, token))
            else selected(value - fitness, storages.tail)
          case None ⇒ None
        }
      }

      atomic { implicit txn ⇒
        val notLoaded = normalizedFitness(fitness)
        val fitnessSum = notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum
        val drawn = Random.default.nextDouble * fitnessSum
        val totalFitness = notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum
        //GliteEnvironment.logger.fine("Total fitness " + totalFitness + " among " + notLoaded.size)
        selected(Random.default.nextDouble * totalFitness, notLoaded.toList) match {
          case Some((storage, token)) ⇒
            for {
              (s, t, _) ← notLoaded
              if (s.id != storage.id)
            } s.releaseToken(t)
            storage -> token
          case None ⇒
            for { (s, t, _) ← notLoaded } s.releaseToken(t)
            retry
        }
      }

    }

  private def getBDII: BDII = new BDII(bdii)

}
