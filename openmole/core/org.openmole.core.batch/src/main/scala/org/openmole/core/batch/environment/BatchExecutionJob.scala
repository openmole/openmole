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
import org.openmole.core.batch.file.URIFileCleaner
import org.openmole.core.implementation.execution.ExecutionJob
import org.openmole.core.model.execution.IExecutionJobId
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.job.IJob
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace

object BatchExecutionJob extends Logger {
  val MinUpdateInterval = new ConfigurationLocation("BatchExecutionJob", "MinUpdateInterval")
  val MaxUpdateInterval = new ConfigurationLocation("BatchExecutionJob", "MaxUpdateInterval")
  val IncrementUpdateInterval = new ConfigurationLocation("BatchExecutionJob", "IncrementUpdateInterval");
 
  Workspace += (MinUpdateInterval, "PT2M")
  Workspace += (MaxUpdateInterval, "PT30M")
  Workspace += (IncrementUpdateInterval, "PT2M")
}


class BatchExecutionJob(val executionEnvironment: BatchEnvironment, job: IJob, id: IExecutionJobId) extends ExecutionJob(executionEnvironment, job, id) with IUpdatableWithVariableDelay {

  import BatchExecutionJob._
    
  var batchJob: BatchJob = null
  val killed = new AtomicBoolean(false)
  var serializedJob: SerializedJob = null
  var _delay: Long = Workspace.preferenceAsDurationInMs(MinUpdateInterval)
 
  @transient
  var copyToEnvironmentExecFuture: Future[SerializedJob] = null
    
  @transient
  var finalizeExecutionFuture: Future[_] = null

  asynchonousCopy
    
  private def updateAndGetState: ExecutionState = {
    if (killed.get) return KILLED
    if (batchJob == null) return READY
    
    val oldState = batchJob.state
    val newState = 
      if (!oldState.isFinal) batchJob.updateState
      else oldState

    if(oldState != newState && newState == KILLED) kill
    state
  }

  override def state =
    if (killed.get) KILLED else if (batchJob == null) READY else batchJob.state

  override def update: Boolean = {
    try {
      val oldState = state
      val newState = updateAndGetState

      newState match {
        case READY =>
          if (asynchonousCopy) {
            _delay = 0
            trySubmit
          }
        case (SUBMITTED | RUNNING | KILLED) => {}
        case FAILED => retry
        case DONE => tryFinalise
      }

      //Compute new refresh delay
      if (oldState != state) {
        initDelay
      } else {
        val newDelay = _delay + Workspace.preferenceAsDurationInMs(IncrementUpdateInterval)
        
        val maxDelay = Workspace.preferenceAsDurationInMs(MaxUpdateInterval)
        _delay = if (newDelay <= maxDelay) newDelay else maxDelay
      }
    } catch {
      case (e: TemporaryErrorException) => logger.log(FINE, "Temporary error durring job update.", e)
      case (e: CancellationException) => logger.log(FINE, "Operation interrupted cause job was killed.", e)
      case (e: ShouldBeKilledException) => 
        logger.log(FINE, "Job should be killed", e)
        kill
      case e =>
        logger.log(WARNING, "Error in job update: " + e.getMessage)
        logger.log(FINE, "Error in job update.", e)
        kill
    }

    return !killed.get
  }

  private def initDelay = {
    _delay = Workspace.preferenceAsDurationInMs(MinUpdateInterval)
  }
    
  private def tryFinalise = {
    if (finalizeExecutionFuture == null) {
      finalizeExecutionFuture = ExecutorService.executorService(ExecutorType.DOWNLOAD).submit(new GetResultFromEnvironment(serializedJob.communicationStorage, batchJob.resultPath, job, executionEnvironment, batchJob))
    }
    if (finalizeExecutionFuture.isDone) {
      finalizeExecutionFuture.get
      finalizeExecutionFuture = null
      kill
    }
  }

  private def asynchonousCopy: Boolean = {
    if (serializedJob == null) {
      if (copyToEnvironmentExecFuture == null) {
        copyToEnvironmentExecFuture = ExecutorService.executorService(ExecutorType.UPLOAD).submit(new CopyToEnvironment(executionEnvironment, job));
      }

      if (copyToEnvironmentExecFuture.isDone) {
        serializedJob = copyToEnvironmentExecFuture.get
        copyToEnvironmentExecFuture = null
      }
    }

    serializedJob != null
  }

  private def trySubmit = {
    val js = executionEnvironment.selectAJobService
    try {
      if(killed.get) throw new InternalProcessingError("Job has been killed")
      //FIXME copyToEnvironmentResult may be null if job killed here
      val bj = js._1.submit(serializedJob, js._2)
      batchJob = bj
    } catch {
      case e => logger.log(FINE, "Error durring job submission.", e)
    } finally {
      JobServiceControl.usageControl(js._1.description).releaseToken(js._2)
    }
  }

  private def clean = {
    if (serializedJob != null) {
      val storage = serializedJob.communicationStorage
      import storage._
      ExecutorService.executorService(ExecutorType.REMOVE).submit(new URIFileCleaner(serializedJob.communicationDirPath.toURIFile, true))
      serializedJob = null
    }
  }

  def kill = {
    
    if (!killed.getAndSet(true)) {
      try {
        val copy = copyToEnvironmentExecFuture
        if (copy != null) copy.cancel(true)
        
        val finalize = finalizeExecutionFuture
        if (finalize != null) finalize.cancel(true)
        
        clean
      } finally {
        val bj = batchJob
        if (bj != null) {
          ExecutorService.executorService(ExecutorType.KILL).submit(new BatchJobKiller(bj))
        }
      }
    }
  }

  def retry = {
    batchJob = null
    _delay = 0
  }

  def delay: Long =  _delay

}
