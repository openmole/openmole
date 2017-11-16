package org.openmole.plugin.environment.batch.refresh
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchExecutionJob, UsageControl }

object StopEnvironmentActor {

  def receive(stop: StopEnvironment)(implicit services: BatchEnvironment.Services) = try {
    stop.environment.jobs.foreach(_.state = ExecutionState.KILLED)

    stop.usageControls.foreach(UsageControl.stop)
    stop.usageControls.foreach(UsageControl.waitUnused)

    val token = UsageControl.faucetToken

    def kill(job: BatchExecutionJob) = {
      job.state = ExecutionState.KILLED
      job.batchJob.foreach(bj ⇒ util.Try(JobManager.killBatchJob(bj, token)))
      job.serializedJob.foreach(sj ⇒ util.Try(JobManager.cleanSerializedJob(sj, token)))
    }

    stop.environment.jobs.foreach(kill)
  }
  finally stop.close.foreach(_())

}
