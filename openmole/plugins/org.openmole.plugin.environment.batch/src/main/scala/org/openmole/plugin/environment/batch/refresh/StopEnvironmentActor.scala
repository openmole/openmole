package org.openmole.plugin.environment.batch.refresh
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchExecutionJob, UsageControl }

object StopEnvironmentActor {

  def receive(stop: StopEnvironment)(implicit services: BatchEnvironment.Services) = try {
    stop.environment.jobs.foreach(_.state = ExecutionState.KILLED)

    stop.usageControls.foreach(UsageControl.freeze)
    stop.usageControls.foreach(UsageControl.waitUnused)
    stop.usageControls.foreach(UsageControl.unfreeze)

    def kill(job: BatchExecutionJob) = {
      job.state = ExecutionState.KILLED
      job.batchJob.foreach { bj ⇒ UsageControl.withToken(bj.usageControl)(token ⇒ util.Try(JobManager.killBatchJob(bj, token))) }
      job.serializedJob.foreach { sj ⇒ UsageControl.withToken(sj.storage.usageControl)(token ⇒ util.Try(JobManager.cleanSerializedJob(sj, token))) }
    }

    val futures = stop.environment.jobs.map { j ⇒ services.threadProvider.submit(() ⇒ kill(j), JobManager.killPriority) }
    futures.foreach(_.get())
  }
  finally stop.close.foreach(_())

}
