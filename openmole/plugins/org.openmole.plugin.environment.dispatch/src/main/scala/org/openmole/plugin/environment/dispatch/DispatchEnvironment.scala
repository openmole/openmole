package org.openmole.plugin.environment.dispatch

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.preference.{ Preference, PreferenceLocation }
import org.openmole.core.threadprovider.{ IUpdatable, Updater }
import org.openmole.core.workflow.execution.ExecutionState.{ ExecutionState, FAILED, KILLED, READY, RUNNING, SUBMITTED, DONE }
import org.openmole.core.workflow.execution.{ Environment, EnvironmentProvider, ExecutionJob, SubmissionEnvironment }
import org.openmole.core.workflow.job.{ JobGroup, MoleJobId }
import org.openmole.core.workflow.tools.ExceptionEvent
import org.openmole.plugin.environment.batch.environment._
import squants.time.Time

import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable

object DispatchEnvironment {
  val updateInterval = PreferenceLocation("DispatchEnvironment", "UpdateInterval", Some(10 seconds))

  object DestinationProvider {
    implicit def toDestinationProvider(on: On[Int, EnvironmentProvider]) = DestinationProvider(on.on, on.value)
  }

  case class DestinationProvider(environment: EnvironmentProvider, slot: Int)
  case class Destination(environment: Environment, slot: Int)

  object State {
    case class Queued(group: JobGroup, id: Long)

    def enqueue(state: State, job: JobGroup, id: Long) = state.synchronized(state.jobQueue.enqueue(Queued(job, id)))
    def dequeue(state: State, n: Int) = state.synchronized { Vector.fill(n)(state.jobQueue.dequeueFirst(_ ⇒ true)).flatten }
    def queueSize(state: State) = state.synchronized { state.jobQueue.size }

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

  case class DispatchJob(moleJobIds: Iterable[MoleJobId]) extends ExecutionJob

  def fillFreeSlots(dispatchEnvironment: DispatchEnvironment) = dispatchEnvironment.state.synchronized {
    def availableSlots(state: State, environments: Seq[DispatchEnvironment.Destination]) =
      environments.map { env ⇒
        val occupied = state.submitted.getOrElse(env.environment, mutable.Map()).size
        env.environment -> (env.slot - occupied)
      }

    for {
      (env, slots) ← availableSlots(dispatchEnvironment.state, dispatchEnvironment.destination)
      _ ← 0 until slots
    } submitToEnvironment(dispatchEnvironment, env)
  }

  def submitToEnvironment(dispatchEnvironment: DispatchEnvironment, environment: Environment) = dispatchEnvironment.state.synchronized {
    State.dequeue(dispatchEnvironment.state, 1) foreach { job ⇒
      val dispatchedId = Environment.submit(environment, job.group)
      State.register(dispatchEnvironment.state, environment, job.id, dispatchedId)
    }
  }

  def jobFinished(dispatchEnvironment: DispatchEnvironment, environment: Environment, id: Long, executionJob: ExecutionJob) =
    State.remove(dispatchEnvironment.state, environment, id) foreach { dispatchId ⇒
      dispatchEnvironment.services.eventDispatcher.trigger(dispatchEnvironment, Environment.JobStateChanged(dispatchId, DispatchJob(executionJob.moleJobIds), SUBMITTED, DONE))
      submitToEnvironment(dispatchEnvironment, environment)
    }

  def apply(
    destination: Seq[DestinationProvider],
    refresh:     OptionalArgument[Time]   = None,
    name:        OptionalArgument[String] = None)(implicit services: BatchEnvironment.Services, varName: sourcecode.Name) = {
    import org.openmole.core.event._
    import services.eventDispatcher

    EnvironmentProvider.multiple { ms ⇒
      val environmentInstances = destination.flatMap(e ⇒ EnvironmentProvider.build(e.environment, ms))
      val environmentInstancesMap = environmentInstances.toMap

      val dispatchEnvironment =
        new DispatchEnvironment(
          destination = destination.map(e ⇒ Destination(environmentInstancesMap(e.environment), e.slot)),
          name = Some(name.getOrElse(varName.value)),
          refresh = refresh,
          services = services.set(ms)
        )

      for {
        env ← environmentInstances
      } {
        env._2 listen {
          case (env, e: Environment.JobStateChanged) ⇒
            def isActive(s: ExecutionState) = s match {
              case READY | SUBMITTED | RUNNING ⇒ true
              case _                           ⇒ false
            }

            if (!isActive(e.newState)) jobFinished(dispatchEnvironment, env, e.id, e.job)
        }
      }

      (dispatchEnvironment, environmentInstances)
    }
  }

}

class DispatchEnvironment(
  val destination: Seq[DispatchEnvironment.Destination],
  val name:        Option[String],
  val refresh:     Option[Time],
  val services:    BatchEnvironment.Services) extends SubmissionEnvironment { env ⇒

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
