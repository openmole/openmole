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

package org.openmole.core.batch.environment

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.dispatch.Dispatchers
import akka.routing.RoundRobinRouter
import com.typesafe.config.ConfigFactory
import java.io.File

import org.openmole.misc.exception.InternalProcessingError
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import org.openmole.core.batch.control._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.environment.BatchJobWatcher.Watch
import org.openmole.core.batch.authentication._
import org.openmole.core.batch.refresh._
import org.openmole.core.batch.replication._
import org.openmole.core.implementation.execution._
import org.openmole.misc.workspace._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.model.job._
import org.openmole.misc.tools.service._
import org.openmole.misc.updater._
import org.openmole.misc.workspace._
import org.openmole.misc.pluginmanager._
import org.openmole.misc.eventdispatcher._
import org.openmole.core.model.execution._
import org.openmole.misc.tools.collection._
import akka.actor.Actor
import akka.actor.Props
import akka.routing.SmallestMailboxRouter
import scala.concurrent.stm._
import collection.mutable.SynchronizedMap
import collection.mutable.WeakHashMap
import org.openmole.misc.tools.service.ThreadUtil._
import concurrent.util.duration._
import ref.WeakReference
import annotation.tailrec

object BatchEnvironment extends Logger {

  trait Transfert {
    def id: Long
  }

  val transfertId = new AtomicLong

  case class BeginUpload(val id: Long, val path: String, val storage: StorageService) extends Event[BatchEnvironment] with Transfert
  case class EndUpload(val id: Long, val path: String, val storage: StorageService) extends Event[BatchEnvironment] with Transfert

  case class BeginDownload(val id: Long, val path: String, val storage: StorageService) extends Event[BatchEnvironment] with Transfert
  case class EndDownload(val id: Long, val path: String, val storage: StorageService) extends Event[BatchEnvironment] with Transfert

  def signalUpload[T](upload: ⇒ T, path: String, storage: StorageService) = {
    val id = transfertId.getAndIncrement
    EventDispatcher.trigger(storage.environment, new BeginUpload(id, path, storage))
    try upload
    finally EventDispatcher.trigger(storage.environment, new EndUpload(id, path, storage))
  }

  def signalDownload[T](download: ⇒ T, path: String, storage: StorageService) = {
    val id = transfertId.getAndIncrement
    EventDispatcher.trigger(storage.environment, new BeginDownload(id, path, storage))
    try download
    finally EventDispatcher.trigger(storage.environment, new EndDownload(id, path, storage))
  }

  val MemorySizeForRuntime = new ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime")
  val RuntimeLocation = new ConfigurationLocation("BatchEnvironment", "RuntimeLocation")
  val JVMLinuxI386Location = new ConfigurationLocation("BatchEnvironment", "JVMLinuxI386Location")

  val JVMLinuxX64Location = new ConfigurationLocation("BatchEnvironment", "JVMLinuxX64Location")

  val MinValueForSelectionExploration = new ConfigurationLocation("BatchEnvironment", "MinValueForSelectionExploration")

  val QualityHysteresis = new ConfigurationLocation("BatchEnvironment", "QualityHysteresis")
  val CheckInterval = new ConfigurationLocation("BatchEnvironment", "CheckInterval")

  val CheckFileExistsInterval = new ConfigurationLocation("BatchEnvironment", "CheckFileExistsInterval")

  val MinUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MinUpdateInterval")
  val MaxUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MaxUpdateInterval")
  val IncrementUpdateInterval = new ConfigurationLocation("BatchEnvironment", "IncrementUpdateInterval");
  val StatisticsHistorySize = new ConfigurationLocation("Environment", "StatisticsHistorySize")

  val JobManagmentThreads = new ConfigurationLocation("BatchEnvironment", "JobManagmentThreads")

  val EnvironmentCleaningThreads = new ConfigurationLocation("BatchEnvironment", "EnvironmentCleaningThreads")

  val StoragesGCUpdateInterval = new ConfigurationLocation("BatchEnvironment", "StoragesGCUpdateInterval")

  //val AuthenticationTimeout = new ConfigurationLocation("Environment", "AuthenticationTimeout")

  Workspace += (MinUpdateInterval, "PT1M")
  Workspace += (MaxUpdateInterval, "PT20M")
  Workspace += (IncrementUpdateInterval, "PT1M")

  Workspace += (RuntimeLocation, () ⇒ new File(new File(Workspace.location, "runtime"), "org.openmole.runtime.tar.gz").getAbsolutePath)
  Workspace += (JVMLinuxI386Location, () ⇒ new File(new File(Workspace.location, "runtime"), "jvm-linux-i386.tar.gz").getAbsolutePath)
  Workspace += (JVMLinuxX64Location, () ⇒ new File(new File(Workspace.location, "runtime"), "jvm-linux-x64.tar.gz").getAbsolutePath)

  Workspace += (MemorySizeForRuntime, "512")
  Workspace += (QualityHysteresis, "100")
  Workspace += (CheckInterval, "PT1M")
  Workspace += (MinValueForSelectionExploration, "0.0001")
  Workspace += (CheckFileExistsInterval, "PT1H")
  Workspace += (StatisticsHistorySize, "10000")
  Workspace += (JobManagmentThreads, "100")
  Workspace += (EnvironmentCleaningThreads, "20")
  //Workspace += (AuthenticationTimeout, "PT2M")

  Workspace += (StoragesGCUpdateInterval, "PT1H")

  def defaultRuntimeMemory = Workspace.preferenceAsInt(BatchEnvironment.MemorySizeForRuntime)
}

import BatchEnvironment._

trait BatchEnvironment extends Environment { env ⇒

  val system = ActorSystem("BatchEnvironment", ConfigFactory.parseString(
    """
akka {
  daemonic="on"
  actor {
    default-dispatcher {
      executor = "fork-join-executor"
      type = Dispatcher
      
      fork-join-executor {
        parallelism-min = 5
        parallelism-max = 10
      }
    }
  }
}
""").withFallback(ConfigFactory.load(classOf[ConfigFactory].getClassLoader)))

  val jobManager = system.actorOf(Props(new JobManager(this)))
  val watcher = system.actorOf(Props(new BatchJobWatcher(this)))

  import system.dispatcher

  system.scheduler.schedule(
    Workspace.preferenceAsDurationInMs(BatchEnvironment.CheckInterval) milliseconds,
    Workspace.preferenceAsDurationInMs(BatchEnvironment.CheckInterval) milliseconds,
    watcher,
    Watch)

  val jobRegistry = new ExecutionJobRegistry
  val statistics = new OrderedSlidingList[StatisticSample](Workspace.preferenceAsInt(StatisticsHistorySize))

  val id: String

  type SS <: StorageService
  type JS <: JobService

  def allStorages: Iterable[SS]
  def allJobServices: Iterable[JS]

  def runtimeMemory: Option[Int] = None
  def runtimeMemoryValue = runtimeMemory match {
    case None ⇒ Workspace.preferenceAsInt(MemorySizeForRuntime)
    case Some(m) ⇒ m
  }

  override def submit(job: IJob) = {
    val bej = executionJob(job)
    EventDispatcher.trigger(this, new IEnvironment.JobSubmitted(bej))
    jobRegistry.register(bej)
    jobManager ! Upload(bej)
  }

  def clean = ReplicaCatalog.withClient { implicit c ⇒
    val cleaningThreadPool = fixedThreadPool(Workspace.preferenceAsInt(EnvironmentCleaningThreads))
    allStorages.foreach {
      s ⇒
        background {
          UsageControl.withToken(s.id) { implicit t ⇒
            s.clean
          }
        }(cleaningThreadPool)
    }
  }

  def executionJob(job: IJob) = new BatchExecutionJob(this, job)

  @transient lazy val runtime = new File(Workspace.preference(BatchEnvironment.RuntimeLocation))
  @transient lazy val jvmLinuxI386 = new File(Workspace.preference(BatchEnvironment.JVMLinuxI386Location))
  @transient lazy val jvmLinuxX64 = new File(Workspace.preference(BatchEnvironment.JVMLinuxX64Location))

  @transient lazy val jobServices = {
    val jobServices = allJobServices
    if (jobServices.isEmpty) throw new InternalProcessingError("No job service available for the environment.")
    jobServices.foreach { s ⇒ UsageControl.register(s.id, UsageControl(s.connections)) }
    jobServices.foreach { s ⇒ JobServiceControl.register(s.id, new JobServiceQualityControl(Workspace.preferenceAsInt(BatchEnvironment.QualityHysteresis))) }
    jobServices
  }

  @transient lazy val storages = {
    val storages = allStorages
    if (storages.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")
    storages.foreach { s ⇒ UsageControl.register(s.id, UsageControl(s.connections)) }
    storages.foreach(s ⇒ StorageControl.register(s.id, new QualityControl(Workspace.preferenceAsInt(BatchEnvironment.QualityHysteresis))))
    Updater.registerForUpdate(new StoragesGC(WeakReference(storages)), Workspace.preferenceAsDurationInMs(StoragesGCUpdateInterval))
    storages
  }

  private def orMinForExploration(v: Double) = {
    val min = Workspace.preferenceAsDouble(BatchEnvironment.MinValueForSelectionExploration)
    if (v < min) min else v
  }

  def selectAJobService: (JobService, AccessToken) = {
    if (jobServices.size == 1) {
      val r = jobServices.head
      return (r, UsageControl.get(r.id).waitAToken)
    }

    def fitness =
      jobServices.flatMap {
        cur ⇒
          UsageControl.get(cur.id).tryGetToken match {
            case None ⇒ None
            case Some(token) ⇒
              val q = JobServiceControl.qualityControl(cur.id).get

              val nbSubmitted = q.submitted
              val fitness = orMinForExploration(
                if (q.submitted > 0)
                  math.pow((q.runnig.toDouble / q.submitted) * q.successRate * (q.totalDone / q.totalSubmitted), 2)
                else math.pow(q.successRate, 2))
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
      val notLoaded = fitness
      selected(Random.default.nextDouble * notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum, notLoaded.toList) match {
        case Some((jobService, token)) ⇒
          for {
            (s, t, _) ← notLoaded
            if (s.id != jobService.id)
          } UsageControl.get(s.id).releaseToken(t)
          jobService -> token
        case None ⇒ retry
      }
    }
  }

  def selectAStorage(usedFiles: Iterable[File]): (StorageService, AccessToken) = {
    if (storages.size == 1) {
      val r = storages.head
      return (r, UsageControl.get(r.id).waitAToken)
    }

    val totalFileSize = usedFiles.map { _.size }.sum
    val onStorage = ReplicaCatalog.withClient(ReplicaCatalog.inCatalog(env.id)(_))

    def fitness =
      storages.flatMap {
        cur ⇒

          UsageControl.get(cur.id).tryGetToken match {
            case None ⇒ None
            case Some(token) ⇒
              val sizeOnStorage = usedFiles.filter(onStorage.getOrElse(_, Set.empty).contains(cur.id)).map(_.size).sum

              val fitness = orMinForExploration(
                (StorageControl.qualityControl(cur.id) match {
                  case Some(q) ⇒ math.pow(q.successRate, 2)
                  case None ⇒ 1.
                }) * (if (totalFileSize != 0) (sizeOnStorage.toDouble / totalFileSize) else 1))
              Some((cur, token, fitness))
          }
      }

    @tailrec def selected(value: Double, storages: List[(StorageService, AccessToken, Double)]): Option[(StorageService, AccessToken)] =
      storages.headOption match {
        case Some((storage, token, fitness)) ⇒
          if (value <= fitness) Some((storage, token))
          else selected(value - fitness, storages.tail)
        case None ⇒ None
      }

    atomic { implicit txn ⇒
      val notLoaded = fitness
      selected(Random.default.nextDouble * notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum, notLoaded.toList) match {
        case Some((storage, token)) ⇒
          for {
            (s, t, _) ← notLoaded
            if (s.id != storage.id)
          } UsageControl.get(s.id).releaseToken(t)
          storage -> token
        case None ⇒ retry
      }
    }

  }

  @transient lazy val plugins = PluginManager.pluginsForClass(this.getClass)

  def minUpdateInterval = Workspace.preferenceAsDurationInMs(MinUpdateInterval)
  def maxUpdateInterval = Workspace.preferenceAsDurationInMs(MaxUpdateInterval)
  def incrementUpdateInterval = Workspace.preferenceAsDurationInMs(IncrementUpdateInterval)

}
