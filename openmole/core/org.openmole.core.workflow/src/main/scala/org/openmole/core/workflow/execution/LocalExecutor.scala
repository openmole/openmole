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

package org.openmole.core.workflow.execution

import java.io.PrintStream
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workflow.execution.Environment.*
import org.openmole.core.workflow.job.*
import org.openmole.core.workflow.mole.MoleExecutionMessage
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.file.*

import scala.ref.WeakReference

object LocalExecutor extends JavaLogger

/**
 * Runnable class for local execution
 *
 * @param environment
 */
class LocalExecutor(environment: WeakReference[LocalEnvironment]) extends Runnable:

  import LocalExecutor.Log._

  var stop: Boolean = false
  @volatile var runningJob: Option[Job] = None

  override def run =
    try
      while !stop
      do
        environment.get match
          case Some(environment) =>
            val executionJob = environment.pool().takeNextJob
            val beginTime = System.currentTimeMillis

            try
              val (log, output) =
                withRedirectedOutput(executionJob, environment.deinterleave):
                  environment.eventDispatcherService.trigger(environment, Environment.JobStateChanged(executionJob.id, executionJob, ExecutionState.RUNNING, ExecutionState.SUBMITTED))

                  for
                    moleJob <- executionJob.jobs
                  do
                    runningJob = Some(moleJob)

                    val taskExecutionDirectory = executionJob.executionContext.moleExecutionDirectory.newDirectory("taskExecution")
                    val result =
                      try
                        val executionContext =
                          TaskExecutionContext.complete(
                            partialTaskExecutionContext = executionJob.executionContext,
                            taskExecutionDirectory = taskExecutionDirectory,
                            localEnvironment = environment
                          )

                        try moleJob.perform(executionContext)
                        finally runningJob = None
                      finally taskExecutionDirectory.recursiveDelete

                    Job.finish(moleJob, result)

                    result match
                      case Right(_: Job.SubMoleCanceled) =>
                        environment.eventDispatcherService.trigger(environment, Environment.JobStateChanged(executionJob.id, executionJob, ExecutionState.KILLED, ExecutionState.RUNNING))
                      case Right(e) =>
                        environment._failed.incrementAndGet()
                        environment.eventDispatcherService.trigger(environment, Environment.JobStateChanged(executionJob.id, executionJob, ExecutionState.FAILED, ExecutionState.RUNNING))
                        environment.eventDispatcherService.trigger(environment: Environment, MoleJobExceptionRaised(executionJob, e, SEVERE, moleJob.id, None))
                      case _ =>
                        environment.eventDispatcherService.trigger(environment, Environment.JobStateChanged(executionJob.id, executionJob, ExecutionState.DONE, ExecutionState.RUNNING))
                        environment._done.incrementAndGet()

                  val endTime = System.currentTimeMillis
                  RuntimeLog(beginTime, beginTime, endTime, endTime)


              output.foreach:
                case Output(stream, output, error) =>
                  display(stream, s"Output of local execution", output)
                  display(stream, s"Error of local execution", error)

              environment.eventDispatcherService.trigger(environment: Environment, Environment.JobCompleted(executionJob, log, RuntimeInfo.localRuntimeInfo))
            catch
              case e: InterruptedException => throw e
              case e: ThreadDeath          => throw e
              case e: Throwable =>
                logger.log(SEVERE, "Error in execution", e)
                executionJob.moleExecution.foreach { me => MoleExecutionMessage.send(me)(MoleExecutionMessage.MoleExecutionError(e)) }
                val er = ExecutionJobExceptionRaised(executionJob, e, SEVERE, None)
                environment.eventDispatcherService.trigger(environment: Environment, er)
          case None => stop = true
    catch
      case e: InterruptedException =>
      case e: ThreadDeath          =>


  case class Output(stream: PrintStream, output: String, error: String)

  private def withRedirectedOutput[T](executionJob: LocalExecutionJob, deinterleave: Boolean)(f: => T) =
    executionJob.moleExecution match
      case Some(execution) if deinterleave =>
        val (res, out) = OutputManager.withStringOutput(f)
        res -> Some(Output(execution.executionContext.services.outputRedirection.output, out.output, out.error))
      case Some(execution) =>
        val res = OutputManager.withStreamOutputs(execution.executionContext.services.outputRedirection.output, execution.executionContext.services.outputRedirection.output)(f)
        res -> None
      case _ =>
        f -> None


