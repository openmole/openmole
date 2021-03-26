package org.openmole.core.workflow.execution

import org.openmole.core.workflow.job.Job
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.task.TaskExecutionContext

/**
 * An [[ExecutionJob]] on the local environment (retrieved from the [[TaskExecutionContext]])
 *
 * @param executionContext
 * @param moleExecution
 */
object LocalExecutionJob {

  def apply(
    id:               Long,
    executionContext: TaskExecutionContext.Partial,
    jobs:             Iterable[Job],
    moleExecution:    Option[MoleExecution]) =
    new LocalExecutionJob(
      id = id,
      executionContext = executionContext,
      jobs = jobs.toArray,
      _moleExecution = moleExecution.getOrElse(null))

}

case class LocalExecutionJob(id: Long, executionContext: TaskExecutionContext.Partial, jobs: Array[Job], _moleExecution: MoleExecution) extends ExecutionJob {
  def moleJobIds = jobs.map(_.id)
  def moleExecution: Option[MoleExecution] = Option(_moleExecution)
}
