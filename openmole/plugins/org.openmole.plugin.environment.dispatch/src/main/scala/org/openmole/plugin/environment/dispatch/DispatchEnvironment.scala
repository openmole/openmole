package org.openmole.plugin.environment.dispatch

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.preference.{ Preference, PreferenceLocation }
import org.openmole.core.workflow.execution.{ExceptionEvent, ExecutionState}
import org.openmole.core.workflow.execution.{ Environment, EnvironmentBuilder, ExecutionJob, SubmissionEnvironment }
import org.openmole.core.workflow.job.{ JobGroup, JobId }
import org.openmole.plugin.environment.batch.environment._
import org.openmole.core.event._
import org.openmole.core.replication.ReplicaCatalog

import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable
import scala.ref.WeakReference

object DispatchEnvironment {
  val updateInterval = PreferenceLocation("DispatchEnvironment", "UpdateInterval", Some(10 seconds))

  object DestinationProvider {
    implicit def toDestinationProvider(on: On[Int, EnvironmentBuilder]): DestinationProvider = DestinationProvider(on.on, on.value)
  }

  case class DestinationProvider(environment: EnvironmentBuilder, slot: Int)

  case class Destination(environment: Environment, slot: Int)

  object State {

    case class Queued(group: JobGroup, id: Long)

    def enqueue(state: State, job: JobGroup, id: Long) = state.synchronized(state.jobQueue.enqueue(Queued(job, id)))

    def dequeue(state: State, n: Int) = state.synchronized {
      Vector.fill(n)(state.jobQueue.dequeueFirst(_ => true)).flatten
    }

    def queueSize(state: State) = state.synchronized {
      state.jobQueue.size
    }

    def register(state: State, environment: Environment, id: Long, dispatchedId: Long) = state.synchronized {
      val idMap = state.submitted.getOrElseUpdate(environment, mutable.Map())
      idMap(dispatchedId) = id
    }

    def remove(state: State, environment: Environment, dispatchedId: Long) = state.synchronized {
      val idMap = state.submitted.getOrElse(environment, mutable.Map())
      idMap.remove(dispatchedId)
    }
  }

  class State {
    val jobQueue = mutable.Queue[State.Queued]()
    val submitted = mutable.Map[Environment, mutable.Map[Long, Long]]()
  }

  case class DispatchJob(moleJobIds: Iterable[JobId]) extends ExecutionJob

  def fillFreeSlots(dispatchEnvironment: DispatchEnvironment) = dispatchEnvironment.state.synchronized {
    def availableSlots(state: State, environments: Seq[DispatchEnvironment.Destination]) =
      environments.map { env =>
        val occupied = state.submitted.getOrElse(env.environment, mutable.Map()).size
        env.environment -> (env.slot - occupied)
      }

    for {
      (env, slots) ← availableSlots(dispatchEnvironment.state, dispatchEnvironment.destination)
      _ ← 0 until slots
    } submitToEnvironment(dispatchEnvironment, env)
  }

  def submitToEnvironment(dispatchEnvironment: DispatchEnvironment, environment: Environment) = dispatchEnvironment.state.synchronized {
    State.dequeue(dispatchEnvironment.state, 1) foreach { job =>
      val dispatchedId = Environment.submit(environment, job.group)
      State.register(dispatchEnvironment.state, environment, job.id, dispatchedId)
    }
  }

  def jobFinished(dispatchEnvironment: DispatchEnvironment, environment: Environment, id: Long, executionJob: ExecutionJob, state: ExecutionState) =
    State.remove(dispatchEnvironment.state, environment, id) foreach { dispatchId =>
      dispatchEnvironment.services.eventDispatcher.trigger(dispatchEnvironment, Environment.JobStateChanged(dispatchId, DispatchJob(executionJob.moleJobIds), ExecutionState.SUBMITTED, state))
      submitToEnvironment(dispatchEnvironment, environment)
    }

  def stateChangedListener(dispatchEnvironment: WeakReference[DispatchEnvironment]): PartialFunction[(Environment, Event[Environment]), Unit] = 
    case (env: Environment, e: Environment.JobStateChanged) =>
      def isActive(s: ExecutionState) = 
        s match 
          case ExecutionState.READY | ExecutionState.SUBMITTED | ExecutionState.RUNNING => true
          case _ => false
        

      if !isActive(e.newState)
      then dispatchEnvironment.get.foreach(dispatch => jobFinished(dispatch, env, e.id, e.job, e.newState))

  def apply(
    slot: Seq[DestinationProvider],
    name: OptionalArgument[String] = None)(implicit replicaCatalog: ReplicaCatalog, varName: sourcecode.Name) =

    EnvironmentBuilder.multiple: (ms, c1) =>
      import ms._

      val c2 = EnvironmentBuilder.build(slot.map(_.environment), ms, c1)

      val dispatchEnvironment =
        new DispatchEnvironment(
          destination = slot.map(e => Destination(c2(e.environment), e.slot)),
          name = Some(name.getOrElse(varName.value)),
          services = BatchEnvironment.Services(ms)
        )

      for
        env <- slot.map(_.environment)
      do
        c2(env).listen(stateChangedListener(WeakReference(dispatchEnvironment)))

      (dispatchEnvironment, c2)


}

class DispatchEnvironment(
  val destination: Seq[DispatchEnvironment.Destination],
  val name:        Option[String],
  val services:    BatchEnvironment.Services) extends SubmissionEnvironment { env =>

  val state = new DispatchEnvironment.State
  private val jobId = new AtomicLong(0L)

  override def start(): Unit = {}
  override def stop(): Unit = {}

  override def submit(job: JobGroup): Long = {
    val id = jobId.getAndIncrement
    DispatchEnvironment.State.enqueue(state, job, id)
    DispatchEnvironment.fillFreeSlots(env)
    id
  }

  override def submitted: Long = DispatchEnvironment.State.queueSize(state)

  override def jobs: Iterable[ExecutionJob] = Seq()
  override def runningJobs: Seq[ExecutionJob] = Seq()
  override def clean: Boolean = true
  override def errors: Seq[ExceptionEvent] = Seq()
  override def clearErrors: Seq[ExceptionEvent] = Seq()
  override def running: Long = destination.map(_.environment.running).sum
  override def done: Long = destination.map(_.environment.done).sum
  override def failed: Long = destination.map(_.environment.failed).sum
}
