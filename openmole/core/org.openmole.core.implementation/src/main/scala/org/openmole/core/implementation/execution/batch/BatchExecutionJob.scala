/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.implementation.execution.batch

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.file.URIFileCleaner
import org.openmole.core.implementation.execution.ExecutionJob
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.execution.IExecutionJobId
import org.openmole.core.model.execution.batch.IBatchExecutionJob
import org.openmole.core.model.execution.batch.IBatchJob
import org.openmole.core.model.execution.batch.IBatchJobService
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
  val LOGGER = Logger.getLogger(BatchExecutionJob.getClass.getName)
    

  Activator.getWorkspace().addToConfigurations(MinUpdateInterval, "PT2M")
  Activator.getWorkspace().addToConfigurations(MaxUpdateInterval, "PT30M")
  Activator.getWorkspace().addToConfigurations(IncrementUpdateInterval, "PT2M")

}


class BatchExecutionJob [JS <: IBatchJobService[_,_]](executionEnvironment: BatchEnvironment[JS], job: IJob, id: IExecutionJobId) extends ExecutionJob[BatchEnvironment[JS]](executionEnvironment, job, id) with IBatchExecutionJob[BatchEnvironment[JS]] with IUpdatableWithVariableDelay {

  import BatchExecutionJob._
    
  var batchJob: IBatchJob = null
  val killed = new AtomicBoolean(false)
  var copyToEnvironmentResult: CopyToEnvironmentResult = null
  var delay: Long = 0//Activator.getWorkspace().getPreferenceAsDurationInMs(BatchExecutionJob.MinUpdateInterval)
 
  @transient
  var copyToEnvironmentExecFuture: Future[CopyToEnvironmentResult] = null
    
  @transient
  var finalizeExecutionFuture: Future[_] = null


  asynchonousCopy()
    

  private def updateAndGetState: ExecutionState = {
    if (killed.get) {
      return KILLED
    }

    if (batchJob == null) {
      return READY;
    }

    val oldState = batchJob.state

    if (!oldState.isFinal) {
      val newState = batchJob.updatedState

      if (oldState == SUBMITED && newState == RUNNING) {
        executionEnvironment.sample(SampleType.WAITING, batchJob.lastStatusDurration, job)
      }
    }
      
    state
  }

  override def state: ExecutionState = {
    if (killed.get) {
      return KILLED
    }
    if (batchJob == null) {
      return READY
    }
    return batchJob.state
  }

  override def update: Boolean = {
    try {
      val oldState = state
      val newState = updateAndGetState

      newState match {
        case READY =>
          if (asynchonousCopy()) {
            delay = 0
            trySubmit
          }
        case (SUBMITED | RUNNING | KILLED) => {}
        case FAILED => retry()
        case DONE => tryFinalise
      }

      //Compute new refresh delay
      if (delay == 0 || oldState != state) {
        initDelay
      } else {
        val newDelay = delay + Activator.getWorkspace().getPreferenceAsDurationInMs(IncrementUpdateInterval);
        if (newDelay <= Activator.getWorkspace().getPreferenceAsDurationInMs(MaxUpdateInterval)) {
          this.delay = newDelay;
        }
      }
            
      //LOGGER.log(Level.FINE, "Refreshed state for {0} old state was {1} new state is {2} next refresh in {3}", new Object[]{toString(), oldState, getState(), delay});


    } catch {
      case (e: CancellationException) => 
        LOGGER.log(Level.FINE, "Operation interrupted cause job was killed.", e)
      case e =>
        kill
        LOGGER.log(Level.WARNING, "Error in job update", e);
    }

    return !killed.get
  }

  private def initDelay = {
    this.delay = Activator.getWorkspace().getPreferenceAsDurationInMs(MinUpdateInterval)
  }
    
  private def tryFinalise = {
    if (finalizeExecutionFuture == null) {
      finalizeExecutionFuture = Activator.getExecutorService.getExecutorService(ExecutorType.DOWNLOAD).submit(new GetResultFromEnvironment(copyToEnvironmentResult.communicationStorage.description, copyToEnvironmentResult.outputFile, job, executionEnvironment, batchJob.lastStatusDurration))
    }
    try {
      if (finalizeExecutionFuture.isDone) {
        finalizeExecutionFuture.get
        finalizeExecutionFuture = null
        kill
      }
    } catch {
      case (e: ExecutionException) => throw new InternalProcessingError(e)
      case (e: InterruptedException) => throw new InternalProcessingError(e)
    } 
  }

  private def asynchonousCopy(): Boolean = {
    if (copyToEnvironmentResult == null) {
      if (copyToEnvironmentExecFuture == null) {
        copyToEnvironmentExecFuture = Activator.getExecutorService.getExecutorService(ExecutorType.UPLOAD).submit(new CopyToEnvironment(executionEnvironment, job));
      }

      try {
        if (copyToEnvironmentExecFuture.isDone()) {
          copyToEnvironmentResult = copyToEnvironmentExecFuture.get();
          copyToEnvironmentExecFuture = null;
        }

      } catch {
        case (ex: ExecutionException) => throw new InternalProcessingError(ex)
        case (ex: InterruptedException) => throw new InternalProcessingError(ex)
      }
    }

    return copyToEnvironmentResult != null;
  }

  private def trySubmit() = {

    val js = executionEnvironment.getAJobService
    try {
      if(killed.get()) throw new InternalProcessingError("Job has been killed")
      //FIXME copyToEnvironmentResult may be null if job killed here
      val bj = js._1.submit(copyToEnvironmentResult.inputFile, copyToEnvironmentResult.outputFile, copyToEnvironmentResult.runtime, js._2)
      batchJob = bj
    } catch {
      case(e: InternalProcessingError) => LOGGER.log(Level.FINE, "Error durring job submission.", e)
    } finally {
      Activator.getBatchRessourceControl().getController(js._1.description).getUsageControl.releaseToken(js._2)
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

        if (copy != null) {
          copy.cancel(true)
        }

        val finalize = finalizeExecutionFuture

        if (finalize != null) {
          finalize.cancel(true)
        }
        clean
      } finally {
        val bj = batchJob
        if (bj != null) {
          Activator.getExecutorService().getExecutorService(ExecutorType.KILL).submit(new BatchJobKiller(bj))
        }
      }
    }
  }

  override def retry() = {
    batchJob = null
    delay = 0
  }

  override def getDelay: Long = {
    return delay
  }
}
