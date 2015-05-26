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

package org.openmole.core.workflow.execution.local

import java.io.{ OutputStream, PrintStream }

import org.openmole.core.event.EventDispatcher
import org.openmole.core.output.OutputManager
import org.openmole.core.tools.service
import org.openmole.core.tools.service.{ Logger, LocalHostName }
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.execution.Environment._
import org.openmole.core.workflow.job.State
import org.openmole.core.workflow.task._
import org.openmole.core.tools.service.Logger
import ref.WeakReference
import org.openmole.core.workflow.mole.{ StrainerTaskDecorator, StrainerCapsule }

object LocalExecutor extends Logger

class LocalExecutor(environment: WeakReference[LocalEnvironment]) extends Runnable {

  import LocalExecutor.Log._

  var stop: Boolean = false

  override def run = {
    while (!stop) {
      environment.get match {
        case Some(environment) ⇒
          def jobGoneIdle() = {
            environment.pool.removeExecuter(this)
            environment.pool.addExecuter()
            stop = true
          }

          val executionJob = environment.pool.takeNextjob

          val beginTime = System.currentTimeMillis

          try {
            val (log, output) =
              withRedirectedOutput(executionJob, environment.deinterleave) {
                executionJob.state = ExecutionState.RUNNING

                for (moleJob ← executionJob.moleJobs) {
                  if (moleJob.state != State.CANCELED) {
                    moleJob.task match {
                      case _: MoleTask ⇒ jobGoneIdle()
                      case t: StrainerTaskDecorator ⇒
                        if (classOf[MoleTask].isAssignableFrom(t.task.getClass)) jobGoneIdle()
                      case _ ⇒
                    }
                    moleJob.perform
                    moleJob.exception match {
                      case Some(e) ⇒ EventDispatcher.trigger(environment: Environment, MoleJobExceptionRaised(executionJob, e, SEVERE, moleJob))
                      case _       ⇒
                    }
                  }
                }

                executionJob.state = ExecutionState.DONE

                val endTime = System.currentTimeMillis
                RuntimeLog(beginTime, beginTime, endTime, endTime)
              }

            output.foreach {
              case Output(stream, output, error) ⇒
                display(stream, s"Output of local execution", output.read)
                display(stream, s"Error of local execution", error.read)
            }

            EventDispatcher.trigger(environment: Environment, Environment.JobCompleted(executionJob, log, service.localRuntimeInfo))
          }
          catch {
            case e: InterruptedException ⇒
              if (!stop) {
                logger.log(WARNING, "Interrupted despite stop is false", e)
                EventDispatcher.trigger(environment: Environment, ExceptionRaised(executionJob, e, SEVERE))
              }
            case e: Throwable ⇒
              logger.log(SEVERE, "Error in execution", e)
              EventDispatcher.trigger(environment: Environment, ExceptionRaised(executionJob, e, SEVERE))
          }
          finally executionJob.state = ExecutionState.KILLED
        case None ⇒ stop = true
      }
    }
  }

  case class Output(stream: PrintStream, output: ExecutorOutput, error: ExecutorOutput)

  private def withRedirectedOutput[T](job: LocalExecutionJob, deinterleave: Boolean)(f: ⇒ T) = {
    val output = redirectOutput(job, deinterleave)
    val res =
      try f finally OutputManager.unregister(Thread.currentThread.getThreadGroup)
    (res, output)
  }

  private def redirectOutput(job: LocalExecutionJob, deinterleave: Boolean): Option[Output] = {
    job.moleExecution match {
      case None ⇒
        OutputManager.unregister(Thread.currentThread.getThreadGroup)
        None
      case Some(ex) ⇒
        if (deinterleave) {
          val output = ExecutorOutput()
          val error = ExecutorOutput()
          OutputManager.redirectOutput(Thread.currentThread.getThreadGroup, output)
          OutputManager.redirectError(Thread.currentThread.getThreadGroup, error)
          Some(Output(ex.executionContext.out, output, error))
        }
        else {
          OutputManager.redirectOutput(Thread.currentThread.getThreadGroup, ex.executionContext.out)
          OutputManager.redirectError(Thread.currentThread.getThreadGroup, ex.executionContext.out)
          None
        }
    }
  }

}
