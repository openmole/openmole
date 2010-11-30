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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.environment

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.batch.control.BatchServiceControl
import org.openmole.core.batch.file.URIFileCleaner
import org.openmole.core.batch.internal.Activator
import org.openmole.core.implementation.execution.ExecutionJob
import org.openmole.core.model.execution.IExecutionJobId
import org.openmole.core.model.execution.SampleType
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.job.IJob
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import org.openmole.misc.workspace.ConfigurationLocation

object BatchExecutionJob {
  val MinUpdateInterval = new ConfigurationLocation("BatchExecutionJob", "MinUpdateInterval")
  val MaxUpdateInterval = new ConfigurationLocation("BatchExecutionJob", "MaxUpdateInterval")
  val IncrementUpdateInterval = new ConfigurationLocation("BatchExecutionJob", "IncrementUpdateInterval");
  val LOGGER = Logger.getLogger(classOf[BatchExecutionJob].getName)

  Activator.getWorkspace += (MinUpdateInterval, "PT2M")
  Activator.getWorkspace += (MaxUpdateInterval, "PT30M")
  Activator.getWorkspace += (IncrementUpdateInterval, "PT2M")
}


class BatchExecutionJob(val executionEnvironment: BatchEnvironment, job: IJob, id: IExecutionJobId) extends ExecutionJob(executionEnvironment, job, id) with IBatchExecutionJob with IUpdatableWithVariableDelay {

  import BatchExecutionJob._
    
  var batchJob: IBatchJob = null
  val killed = new AtomicBoolean(false)
  var copyToEnvironmentResult: CopyToEnvironmentResult = null
  var _delay: Long = Activator.getWorkspace.preferenceAsDurationInMs(MinUpdateInterval)
 
  @transient
  var copyToEnvironmentExecFuture: Future[CopyToEnvironmentResult] = null
    
  @transient
  var finalizeExecutionFuture: Future[_] = null

  asynchonousCopy
    
  private def updateAndGetState: ExecutionState = {
    if (killed.get) return KILLED
    if (batchJob == null) return READY
    
    val oldState = batchJob.state

    if (!oldState.isFinal) {
      val newState = batchJob.updatedState

      if (oldState == SUBMITED && newState == RUNNING) {
        executionEnvironment.sample(SampleType.WAITING, batchJob.lastStateDurration, job)
      }
    }
      
    state
  }

  override def state: ExecutionState = {
    //LOGGER.info("Get the state " + killed.get)
    if (killed.get) return KILLED
    if (batchJob == null) return READY
    
    return batchJob.state
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
        case (SUBMITED | RUNNING | KILLED) => {}
        case FAILED => retry
        case DONE => tryFinalise
      }

      //Compute new refresh delay
      if (oldState != state) {
        initDelay
      } else {
        val newDelay = _delay + Activator.getWorkspace.preferenceAsDurationInMs(IncrementUpdateInterval)
        if (newDelay <= Activator.getWorkspace.preferenceAsDurationInMs(MaxUpdateInterval)) {
          _delay = newDelay
        }
      }
            
      //LOGGER.log(Level.FINE, "Refreshed state for {0} old state was {1} new state is {2} next refresh in {3}", new Object[]{toString(), oldState, getState(), delay});


    } catch {
      case (e: CancellationException) => LOGGER.log(Level.FINE, "Operation interrupted cause job was killed.", e)
      case e =>
        kill
        LOGGER.log(Level.WARNING, "Error in job update", e)
    }

    return !killed.get
  }

  private def initDelay = {
    _delay = Activator.getWorkspace.preferenceAsDurationInMs(MinUpdateInterval)
  }
    
  private def tryFinalise = {
    if (finalizeExecutionFuture == null) {
      finalizeExecutionFuture = Activator.getExecutorService.getExecutorService(ExecutorType.DOWNLOAD).submit(new GetResultFromEnvironment(copyToEnvironmentResult.communicationStorage.description, copyToEnvironmentResult.outputFile, job, executionEnvironment, batchJob.lastStateDurration))
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
        copyToEnvironmentExecFuture = Activator.getExecutorService.getExecutorService(ExecutorType.UPLOAD).submit(new CopyToEnvironment(executionEnvironment, job));
      }

      if (copyToEnvironmentExecFuture.isDone) {
        copyToEnvironmentResult = copyToEnvironmentExecFuture.get
        copyToEnvironmentExecFuture = null
      }
    }

    return copyToEnvironmentResult != null
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
      BatchServiceControl.usageControl(js._1.description).releaseToken(js._2)
    }
  }

  private def clean = {
    if (copyToEnvironmentResult != null) {
      Activator.getExecutorService.getExecutorService(ExecutorType.REMOVE).submit(new URIFileCleaner(copyToEnvironmentResult.communicationDir, true))
      copyToEnvironmentResult = null
    }
  }

  override def kill = {
    
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
          Activator.getExecutorService.getExecutorService(ExecutorType.KILL).submit(new BatchJobKiller(bj))
        }
      }
    }
  }

  override def retry = {
    batchJob = null
    _delay = 0
  }

  override def delay: Long =  _delay

}
