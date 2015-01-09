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

package org.openmole.core.workflow.mole

import java.util.UUID
import java.util.concurrent.{ Executors, Executor, Semaphore }
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.job.IMoleJob.moleJobOrdering
import org.openmole.misc.exception._
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.exception.MultipleException
import org.openmole.misc.eventdispatcher._
import org.openmole.misc.tools.service.Priority
import org.openmole.core.workflow.execution.local._
import org.openmole.misc.tools.collection._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.tools._
import org.openmole.misc.tools.service.Random
import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._

import scala.collection.immutable.HashMap
import scala.collection.mutable.Buffer
import scala.concurrent.stm.{ Ref, TMap, atomic, retry }
import javax.xml.bind.annotation.XmlTransient
import org.openmole.core.workflow.execution.Environment
import collection.mutable
import java.io.File
import org.openmole.core.serializer.SerialiserService
import org.openmole.core.workflow.mole

object MoleExecution extends Logger {

  def apply(
    mole: IMole,
    sources: Iterable[(ICapsule, ISource)] = Iterable.empty,
    hooks: Iterable[(ICapsule, IHook)] = Iterable.empty,
    environments: Map[ICapsule, Environment] = Map.empty,
    grouping: Map[ICapsule, Grouping] = Map.empty,
    implicits: Context = Context.empty,
    seed: Long = Workspace.newSeed,
    defaultEnvironment: Environment = LocalEnvironment.default)(implicit executionContext: ExecutionContext) =
    PartialMoleExecution(
      mole,
      sources,
      hooks,
      environments,
      grouping,
      seed,
      defaultEnvironment).toExecution(implicits)(executionContext)

}

class MoleExecution(
    val mole: IMole,
    val sources: Sources,
    val hooks: Hooks,
    val environments: Map[ICapsule, Environment],
    val grouping: Map[ICapsule, Grouping],
    val seed: Long,
    val defaultEnvironment: Environment,
    override val id: String = UUID.randomUUID().toString)(val implicits: Context, val executionContext: ExecutionContext) extends IMoleExecution {

  import IMoleExecution._
  import MoleExecution._

  private val _started = Ref(false)
  private val _canceled = Ref(false)
  private val _finished = Ref(false)

  private val _startTime = Ref(None: Option[Long])
  private val _endTime = Ref(None: Option[Long])

  private val ticketNumber = Ref(0L)

  private val waitingJobs: TMap[ICapsule, TMap[IMoleJobGroup, Ref[List[IMoleJob]]]] =
    TMap(grouping.map { case (c, g) ⇒ c -> TMap.empty[IMoleJobGroup, Ref[List[IMoleJob]]] }.toSeq: _*)

  private val nbWaiting = Ref(0)

  val rootSubMoleExecution = new SubMoleExecution(None, this)
  val rootTicket = Ticket(id, ticketNumber.next)

  val dataChannelRegistry = new RegistryWithTicket[IDataChannel, Buffer[Variable[_]]]

  val _exceptions = Ref(List.empty[Throwable])

  def numberOfJobs = rootSubMoleExecution.numberOfJobs

  def exceptions = _exceptions.single()

  def duration: Option[Long] =
    (_startTime.single(), _endTime.single()) match {
      case (None, _)          ⇒ None
      case (Some(t), None)    ⇒ Some(System.currentTimeMillis - t)
      case (Some(s), Some(e)) ⇒ Some(e - s)
    }

  def group(moleJob: IMoleJob, capsule: ICapsule, submole: ISubMoleExecution) =
    atomic { implicit txn ⇒
      grouping.get(capsule) match {
        case Some(strategy) ⇒
          val groups = waitingJobs(capsule)
          val category = strategy(moleJob.context, TMap.asMap(groups).map { case (gr, jobs) ⇒ gr -> jobs() })
          val jobs = groups.getOrElseUpdate(category, Ref(List.empty))
          jobs() = moleJob :: jobs()
          nbWaiting += 1

          if (strategy.complete(jobs())) {
            groups -= category
            nbWaiting -= jobs().size
            Some(new Job(this, jobs()) -> capsule)
          }
          else None
        case None ⇒
          val job = new Job(this, List(moleJob))
          Some(job -> capsule)
      }
    }.map { case (j, c) ⇒ submit(j, c) }

  private def submit(job: IJob, capsule: ICapsule) =
    if (!job.finished) {
      val env = environments.getOrElse(capsule, defaultEnvironment)
      env.submit(job)
      EventDispatcher.trigger(this, new IMoleExecution.JobSubmitted(job, capsule, env))
    }

  def submitAll =
    atomic { implicit txn ⇒
      val jobs =
        for {
          (capsule, groups) ← TMap.asMap(waitingJobs).toList
          (_, jobs) ← TMap.asMap(groups).toList
        } yield capsule -> jobs()
      nbWaiting() = 0
      waitingJobs.clear
      jobs
    }.foreach {
      case (capsule, jobs) ⇒ submit(new Job(this, jobs), capsule)
    }

  def allWaiting = atomic { implicit txn ⇒ numberOfJobs <= nbWaiting() }

  def start(context: Context): this.type = {
    EventDispatcher.trigger(this, new IMoleExecution.Starting)
    executionContext.directory.foreach(_.mkdirs)
    rootSubMoleExecution.newChild.submit(mole.root, context, nextTicket(rootTicket))
    if (allWaiting) submitAll
    this
  }

  override def start = {
    if (!_started.getUpdate(_ ⇒ true)) {
      val validationErrors = Validation(mole, implicits, sources, hooks)
      if (!validationErrors.isEmpty) throw new UserBadDataError("Formal validation of your mole has failed, several errors have been found: " + validationErrors.mkString("\n"))
      _startTime.single() = Some(System.currentTimeMillis)
      start(Context.empty)
    }
    this
  }

  override def cancel: this.type = {
    if (!_canceled.getUpdate(_ ⇒ true)) {
      rootSubMoleExecution.cancel
      EventDispatcher.trigger(this, new IMoleExecution.Finished)
      _finished.single() = true
      _endTime.single() = Some(System.currentTimeMillis)
    }
    this
  }

  override def moleJobs = rootSubMoleExecution.jobs

  override def waitUntilEnded = {
    atomic { implicit txn ⇒
      if (!_finished()) retry
      if (!_exceptions().isEmpty) throw new MultipleException(_exceptions().reverse)
    }
    this
  }

  def jobFailedOrCanceled(moleJob: IMoleJob, capsule: ICapsule) = {
    moleJob.exception match {
      case None ⇒
      case Some(e) ⇒
        atomic { implicit txn ⇒ _exceptions() = e :: _exceptions() }
    }
    jobOutputTransitionsPerformed(moleJob, capsule)
  }

  def jobOutputTransitionsPerformed(job: IMoleJob, capsule: ICapsule) =
    if (!_canceled.single()) {
      if (allWaiting) submitAll
      if (numberOfJobs == 0) {
        EventDispatcher.trigger(this, new IMoleExecution.Finished)
        _finished.single() = true
        _endTime.single() = Some(System.currentTimeMillis)
      }
    }

  override def finished: Boolean = _finished.single()

  override def started: Boolean = _started.single()

  override def nextTicket(parent: ITicket): ITicket = Ticket(parent, ticketNumber.next)

  def nextJobId = UUID.randomUUID

  def newSeed = rng.nextLong

  lazy val rng = Random.newRNG(seed)

}
