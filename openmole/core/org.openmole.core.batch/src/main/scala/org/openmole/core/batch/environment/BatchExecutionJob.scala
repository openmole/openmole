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
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.misc.executorservice.ExecutorService
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.batch.control.BatchJobServiceControl
import org.openmole.core.batch.file.URIFileCleaner
import org.openmole.core.implementation.execution.ExecutionJob
import org.openmole.core.model.execution.IExecutionJobId
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.job.IJob
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace

object BatchExecutionJob {
  val MinUpdateInterval = new ConfigurationLocation("BatchExecutionJob", "MinUpdateInterval")
  val MaxUpdateInterval = new ConfigurationLocation("BatchExecutionJob", "MaxUpdateInterval")
  val IncrementUpdateInterval = new ConfigurationLocation("BatchExecutionJob", "IncrementUpdateInterval");
  val LOGGER = Logger.getLogger(classOf[BatchExecutionJob].getName)

  Workspace += (MinUpdateInterval, "PT2M")
  Workspace += (MaxUpdateInterval, "PT30M")
  Workspace += (IncrementUpdateInterval, "PT2M")
}


class BatchExecutionJob(val executionEnvironment: BatchEnvironment, job: IJob, id: IExecutionJobId) extends ExecutionJob(executionEnvironment, job, id) with IUpdatableWithVariableDelay {

  import BatchExecutionJob._
    
  var batchJob: BatchJob = null
  val killed = new AtomicBoolean(false)
  var copyToEnvironmentResult: CopyToEnvironmentResult = null
  var _delay: Long = Workspace.preferenceAsDurationInMs(MinUpdateInterval)
 
  @transient
  var copyToEnvironmentExecFuture: Future[CopyToEnvironmentResult] = null
    
  @transient
  var finalizeExecutionFuture: Future[_] = null

  asynchonousCopy
    
  private def updateAndGetState: ExecutionState = {
    if (killed.get) return KILLED
    if (batchJob == null) return READY
    
    val oldState = batchJob.state
    batchJob.updateState

    if(oldState != batchJob.state && batchJob.state == KILLED) kill
    state
  }

  override def state: ExecutionState = {
    if (killed.get) return KILLED
    if (batchJob == null) return READY
    batchJob.state
  }

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
      case (e: TemporaryErrorException) => LOGGER.log(Level.FINE, "Temporary error durring job update.", e)
      case (e: CancellationException) => LOGGER.log(Level.FINE, "Operation interrupted cause job was killed.", e)
      case (e: ShouldBeKilledException) => 
        LOGGER.log(Level.FINE, e.getMessage)
        kill
      case e =>
        LOGGER.log(Level.WARNING, "Error in job update", e)
        kill
    }

    return !killed.get
  }

  private def initDelay = {
    _delay = Workspace.preferenceAsDurationInMs(MinUpdateInterval)
  }
    
  private def tryFinalise = {
    if (finalizeExecutionFuture == null) {
      finalizeExecutionFuture = ExecutorService.executorService(ExecutorType.DOWNLOAD).submit(new GetResultFromEnvironment(copyToEnvironmentResult.communicationStorage.description, copyToEnvironmentResult.outputFile, job, executionEnvironment, batchJob))
    }
    if (finalizeExecutionFuture.isDone) {
      finalizeExecutionFuture.get
      finalizeExecutionFuture = null
      kill
    }
  }

  private def asynchonousCopy: Boolean = {
    if (copyToEnvironmentResult == null) {
      if (copyToEnvironmentExecFuture == null) {
        copyToEnvironmentExecFuture = ExecutorService.executorService(ExecutorType.UPLOAD).submit(new CopyToEnvironment(executionEnvironment, job));
      }

      if (copyToEnvironmentExecFuture.isDone) {
        copyToEnvironmentResult = copyToEnvironmentExecFuture.get
        copyToEnvironmentExecFuture = null
      }
    }

    copyToEnvironmentResult != null
  }

  private def trySubmit = {
    val js = executionEnvironment.selectAJobService
    try {
      if(killed.get) throw new InternalProcessingError("Job has been killed")
      //FIXME copyToEnvironmentResult may be null if job killed here
      val bj = js._1.submit(copyToEnvironmentResult.inputFile, copyToEnvironmentResult.outputFile, copyToEnvironmentResult.runtime, js._2)
      batchJob = bj
    } catch {
      case e => LOGGER.log(Level.FINE, "Error durring job submission.", e)
    } finally {
      BatchJobServiceControl.usageControl(js._1.description).releaseToken(js._2)
    }
  }

  private def clean = {
    if (copyToEnvironmentResult != null) {
      ExecutorService.executorService(ExecutorType.REMOVE).submit(new URIFileCleaner(copyToEnvironmentResult.communicationDir, true))
      copyToEnvironmentResult = null
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
