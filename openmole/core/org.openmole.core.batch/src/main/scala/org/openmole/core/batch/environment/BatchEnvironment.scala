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
import java.util.concurrent.TimeoutException
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
import scala.concurrent.duration.{ Duration ⇒ SDuration, MILLISECONDS }
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

  val CheckInterval = new ConfigurationLocation("BatchEnvironment", "CheckInterval")

  val CheckFileExistsInterval = new ConfigurationLocation("BatchEnvironment", "CheckFileExistsInterval")

  val MinUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MinUpdateInterval")
  val MaxUpdateInterval = new ConfigurationLocation("BatchEnvironment", "MaxUpdateInterval")
  val IncrementUpdateInterval = new ConfigurationLocation("BatchEnvironment", "IncrementUpdateInterval");

  val JobManagmentThreads = new ConfigurationLocation("BatchEnvironment", "JobManagmentThreads")

  val EnvironmentCleaningThreads = new ConfigurationLocation("BatchEnvironment", "EnvironmentCleaningThreads")

  val StoragesGCUpdateInterval = new ConfigurationLocation("BatchEnvironment", "StoragesGCUpdateInterval")

  val NbTryOnTimeout = new ConfigurationLocation("BatchEnvironment", "NbTryOnTimeout")
  val StatisticsHistorySize = new ConfigurationLocation("BatchEnvironment", "StatisticsHistorySize")

  Workspace += (MinUpdateInterval, "PT1M")
  Workspace += (MaxUpdateInterval, "PT20M")
  Workspace += (IncrementUpdateInterval, "PT1M")

  Workspace += (RuntimeLocation, () ⇒ new File(new File(Workspace.location, "runtime"), "org.openmole.runtime.tar.gz").getAbsolutePath)
  Workspace += (JVMLinuxI386Location, () ⇒ new File(new File(Workspace.location, "runtime"), "jvm-linux-i386.tar.gz").getAbsolutePath)
  Workspace += (JVMLinuxX64Location, () ⇒ new File(new File(Workspace.location, "runtime"), "jvm-linux-x64.tar.gz").getAbsolutePath)

  Workspace += (MemorySizeForRuntime, "512")
  Workspace += (CheckInterval, "PT1M")
  Workspace += (CheckFileExistsInterval, "PT1H")
  Workspace += (JobManagmentThreads, "200")
  Workspace += (EnvironmentCleaningThreads, "20")

  Workspace += (StoragesGCUpdateInterval, "PT1H")
  Workspace += (NbTryOnTimeout, "3")
  Workspace += (StatisticsHistorySize, "10000")

  def defaultRuntimeMemory = Workspace.preferenceAsInt(BatchEnvironment.MemorySizeForRuntime)

  def retryOnTimeout[T](f: ⇒ T) = Retry.retryOnTimeout(f, Workspace.preferenceAsInt(NbTryOnTimeout))

}

import BatchEnvironment._

trait BatchEnvironment extends Environment { env ⇒

  @transient lazy val system = ActorSystem("BatchEnvironment", ConfigFactory.parseString(
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

  @transient lazy val jobManager = system.actorOf(Props(new JobManager(this)))
  @transient lazy val watcher = system.actorOf(Props(new BatchJobWatcher(this)))

  import system.dispatcher

  @transient lazy val registerWatcher: Unit = {
    system.scheduler.schedule(
      SDuration(Workspace.preferenceAsDurationInMs(BatchEnvironment.CheckInterval), MILLISECONDS),
      SDuration(Workspace.preferenceAsDurationInMs(BatchEnvironment.CheckInterval), MILLISECONDS),
      watcher,
      Watch)
  }

  val jobRegistry = new ExecutionJobRegistry
  @transient lazy val statistics = new OrderedSlidingList[StatisticSample](Workspace.preferenceAsInt(StatisticsHistorySize))

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
    registerWatcher
    val bej = executionJob(job)
    EventDispatcher.trigger(this, new Environment.JobSubmitted(bej))
    jobRegistry.register(bej)
    jobManager ! Upload(bej)
  }

  def clean = ReplicaCatalog.withClient { implicit c ⇒
    val cleaningThreadPool = fixedThreadPool(Workspace.preferenceAsInt(EnvironmentCleaningThreads))
    allStorages.foreach {
      s ⇒
        background {
          s.withToken { implicit t ⇒
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
    jobServices
  }

  @transient lazy val storages = {
    val storages = allStorages
    if (storages.isEmpty) throw new InternalProcessingError("No storage service available for the environment.")
    Updater.registerForUpdate(new StoragesGC(WeakReference(storages)), Workspace.preferenceAsDurationInMs(StoragesGCUpdateInterval))
    storages
  }

  def selectAJobService: (JobService, AccessToken) = {
    val r = jobServices.head
    (r, r.usageControl.waitAToken)
  }

  def selectAStorage(usedFiles: Iterable[File]): (StorageService, AccessToken) = {
    val r = storages.head
    (r, r.usageControl.waitAToken)
  }

  @transient lazy val plugins = PluginManager.pluginsForClass(this.getClass)

  def minUpdateInterval = Workspace.preferenceAsDurationInMs(MinUpdateInterval)
  def maxUpdateInterval = Workspace.preferenceAsDurationInMs(MaxUpdateInterval)
  def incrementUpdateInterval = Workspace.preferenceAsDurationInMs(IncrementUpdateInterval)

}
