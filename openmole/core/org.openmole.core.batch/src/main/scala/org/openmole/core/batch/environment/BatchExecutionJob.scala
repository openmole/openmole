/*
 * Copyright (C) 2010 reuillon
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

import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import org.openmole.misc.executorservice.ExecutorService
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.batch.control.JobServiceControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.file.URIFileCleaner
import org.openmole.core.implementation.execution.ExecutionJob
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.job.IJob
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.implementation.tools.TimeStamp
import BatchEnvironment._

object BatchExecutionJob extends Logger

class BatchExecutionJob(val executionEnvironment: BatchEnvironment, job: IJob) extends ExecutionJob(executionEnvironment, job) with IUpdatableWithVariableDelay {

  import executionEnvironment.{minUpdateInterval, incrementUpdateInterval, maxUpdateInterval}
  
  timeStamps += new TimeStamp(READY)
  
  import BatchExecutionJob._
    
  var batchJob: Option[BatchJob] = None
  
  val copyToEnvironmentExecFuture: Future[SerializedJob] = 
    ExecutorService.executorService(ExecutorType.UPLOAD).submit(new CopyToEnvironment(executionEnvironment, job))
  
  var finalizeExecutionFuture: Option[Future[_]] = None
  val killed = new AtomicBoolean(false)
  var _delay = minUpdateInterval

  
  private def updateAndGetState = {
    if (killed.get) KILLED
    else batchJob match {
      case None => READY
      case Some(batchJob) =>
        batchJob.updateState
        state
      }
  }
  
  override def state =
    if (killed.get) KILLED 
    else batchJob match {
      case None => READY
      case Some(batchJob) => batchJob.state
    }

  override def update: Boolean = synchronized {
    val oldState = state
    try {
      if(oldState != KILLED) {
        def incrementedDelay = {
          val newDelay = _delay + incrementUpdateInterval
          val maxDelay = maxUpdateInterval
          if (newDelay <= maxDelay) newDelay else maxDelay
        }
      
        _delay = batchJob match {
          case None =>
            if (copyToEnvironmentExecFuture.isDone) {
              batchJob = trySubmit(copyToEnvironmentExecFuture.get)
              Workspace.preferenceAsDurationInMs(MinUpdateInterval)
            } else incrementedDelay
          case Some(batchJob) =>
            val newState = updateAndGetState
 
            newState match {
              case READY => throw new InternalProcessingError("Bug, it should never append.")
              case (SUBMITTED | RUNNING | KILLED) => {}
              case FAILED => resubmit
              case DONE => tryFinalise(batchJob)
            }
            
            if(oldState != newState && newState == DONE) executionEnvironment.statistics += new StatisticSample(batchJob)
            if(oldState != newState) minUpdateInterval
            else incrementedDelay
        }
        
      }

    } catch {
      case (e: TemporaryErrorException) => logger.log(FINE, "Temporary error durring job update.", e)
      case (e: CancellationException) => logger.log(FINE, "Operation interrupted cause job was killed.", e)
      case (e: ShouldBeKilledException) => kill
      case e =>
        EventDispatcher.trigger(executionEnvironment: IEnvironment, new IEnvironment.ExceptionRaised(this, e, WARNING))
        logger.log(WARNING, "Error in job update: " + e.getMessage)
        logger.log(FINE, "Stack of the error in job update" , e)
        kill
    }
    val newState = state
    if(oldState != newState) {
      timeStamps += new TimeStamp(newState)
      EventDispatcher.trigger(executionEnvironment: IEnvironment, new IEnvironment.JobStateChanged(this, newState, oldState))
    }
    !killed.get
  }
    
  private def tryFinalise(batchJob: BatchJob) = 
    finalizeExecutionFuture match {
      case None => 
        finalizeExecutionFuture = Some(ExecutorService.executorService(ExecutorType.DOWNLOAD).
                                       submit(new GetResultFromEnvironment(copyToEnvironmentExecFuture.get.communicationStorage, 
                                                                           batchJob.resultPath, 
                                                                           job, 
                                                                           executionEnvironment, 
                                                                           this)))
      case Some(f) => 
        if (f.isDone) {
          f.get
          kill
        }
    }

  private def trySubmit(serializedJob: SerializedJob) = {
    val (js, token) = executionEnvironment.selectAJobService
    try {
      if(killed.get) throw new InternalProcessingError("Job has been killed")
      Some(js.submit(serializedJob, token))
    } catch {
      case e => 
        EventDispatcher.trigger(executionEnvironment: IEnvironment, new IEnvironment.ExceptionRaised(this, e, FINE))
        logger.log(FINE, "Error durring job submission.", e)
        None
    } finally UsageControl.get(js.description).releaseToken(token)
  }

  private def clean =
    if(copyToEnvironmentExecFuture.isDone && !copyToEnvironmentExecFuture.isCancelled) {
      try {
        val serializedJob = copyToEnvironmentExecFuture.get
        val storage = serializedJob.communicationStorage
        import storage._

        ExecutorService.executorService(ExecutorType.REMOVE).submit(new URIFileCleaner(serializedJob.communicationDirPath.toURIFile, true))
      } catch {
        case e => 
          EventDispatcher.trigger(executionEnvironment: IEnvironment, new IEnvironment.ExceptionRaised(this, e, FINE))
          logger.log(FINE, "Error durring job cleaning.", e)
      }
    }

  def kill = {
    val oldState = state
    if (!killed.getAndSet(true)) {
      try {
        EventDispatcher.trigger(executionEnvironment: IEnvironment, new IEnvironment.JobStateChanged(this, KILLED, oldState))
        copyToEnvironmentExecFuture.cancel(true)      
        clean
      } finally {
        batchJob match {
          case Some(bj) => ExecutorService.executorService(ExecutorType.KILL).submit(new BatchJobKiller(bj))
          case None =>
        }
      }
    }
  }

  private def resubmit = {
    batchJob = None
    _delay = minUpdateInterval
  }

  def delay: Long = _delay

}
