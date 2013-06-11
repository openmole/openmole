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

package org.openmole.core.implementation.mole

import java.util.UUID
import java.util.concurrent.{ Executors, Executor, Semaphore }
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.implementation.data._
import org.openmole.core.model.mole._
import org.openmole.core.implementation.validation._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.job.IMoleJob.moleJobOrdering
import org.openmole.misc.exception._
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.exception.MultipleException
import org.openmole.misc.eventdispatcher._
import org.openmole.misc.tools.service.Priority
import org.openmole.core.implementation.execution.local._
import org.openmole.misc.tools.collection._
import org.openmole.core.implementation.job._
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.service.Random
import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._

import scala.collection.immutable.HashMap
import scala.collection.mutable.Buffer
import scala.concurrent.stm.{ Ref, TMap, atomic, retry }
import javax.xml.bind.annotation.XmlTransient
import org.openmole.core.model.execution.Environment
import collection.mutable
import java.io.File
import org.openmole.core.serializer.SerializerService
import org.openmole.core.model.mole

object MoleExecution extends Logger {

  def apply(
    mole: IMole,
    sources: Iterable[(ICapsule, ISource)] = Iterable.empty,
    hooks: Iterable[(ICapsule, IHook)] = Iterable.empty,
    selection: Map[ICapsule, EnvironmentSelection] = Map.empty,
    grouping: Map[ICapsule, Grouping] = Map.empty,
    profiler: Profiler = Profiler.empty,
    implicits: Context = Context.empty,
    seed: Long = Workspace.newSeed,
    executionContext: ExecutionContext = ExecutionContext.local) =
    PartialMoleExecution(
      mole,
      sources,
      hooks,
      selection,
      grouping,
      profiler,
      seed).toExecution(implicits, executionContext)

}

class MoleExecution(
    val mole: IMole,
    val sources: Sources = Sources.empty,
    val hooks: Hooks = Hooks.empty,
    val selection: Map[ICapsule, EnvironmentSelection] = Map.empty,
    val grouping: Map[ICapsule, Grouping] = Map.empty,
    val profiler: Profiler = Profiler.empty,
    seed: Long = Workspace.newSeed)(implicit val implicits: Context = Context.empty, implicit val executionContext: ExecutionContext = ExecutionContext.local) extends IMoleExecution {

  import IMoleExecution._
  import MoleExecution._

  private val _started = Ref(false)
  private val _canceled = Ref(false)
  private val _finished = Ref(false)

  override val id = UUID.randomUUID.toString

  private val ticketNumber = Ref(0L)
  private val jobId = Ref(0L)

  private val waitingJobs: TMap[ICapsule, TMap[IMoleJobGroup, Ref[List[IMoleJob]]]] =
    TMap(grouping.map { case (c, g) ⇒ c -> TMap.empty[IMoleJobGroup, Ref[List[IMoleJob]]] }.toSeq: _*)

  private val nbWaiting = Ref(0)

  val rootSubMoleExecution = new SubMoleExecution(None, this)
  val rootTicket = Ticket(id, ticketNumber.next)

  val dataChannelRegistry = new RegistryWithTicket[IDataChannel, Buffer[Variable[_]]]

  val _exceptions = Ref(List.empty[Throwable])

  def numberOfJobs = rootSubMoleExecution.numberOfJobs

  def exceptions = _exceptions.single()

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
      val env =
        selection.get(capsule) match {
          case Some(selection) ⇒ selection(job)
          case None            ⇒ LocalEnvironment
        }
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
    rootSubMoleExecution.newChild.submit(mole.root, context, nextTicket(rootTicket))
    if (allWaiting) submitAll
    this
  }

  override def start = {
    if (!_started.getUpdate(_ ⇒ true)) {
      val validationErrors = Validation(mole, implicits, sources, hooks)
      if (!validationErrors.isEmpty) throw new UserBadDataError("Formal validation of your mole has failed, several errors have been found: " + validationErrors.mkString("\n"))
      start(Context.empty)
      _started.single() = true
    }
    this
  }

  override def cancel: this.type = {
    if (!_canceled.getUpdate(_ ⇒ true)) {
      rootSubMoleExecution.cancel
      EventDispatcher.trigger(this, new IMoleExecution.Finished)
      profiler.finished
      _finished.single() = true
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
        profiler.finished
        _finished.single() = true
      }
    }

  override def finished: Boolean = _finished.single()

  override def started: Boolean = _started.single()

  override def nextTicket(parent: ITicket): ITicket = Ticket(parent, ticketNumber.next)

  def nextJobId = new MoleJobId(id, jobId.next)

  def newSeed = rng.nextLong

  lazy val rng = Random.newRNG(seed)

}
