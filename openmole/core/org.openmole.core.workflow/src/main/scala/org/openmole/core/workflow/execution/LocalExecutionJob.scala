package org.openmole.core.workflow.execution

import org.openmole.core.workflow.job.MoleJob
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
    executionContext: TaskExecutionContext,
    jobs:             Iterable[MoleJob],
    moleExecution:    Option[MoleExecution]) =
    new LocalExecutionJob(
      executionContext = executionContext,
      jobs = jobs.toArray,
      _moleExecution = moleExecution.getOrElse(null))
}

case class LocalExecutionJob(executionContext: TaskExecutionContext, jobs: Array[MoleJob], _moleExecution: MoleExecution) extends ExecutionJob {
  def moleJobIds = jobs.map(_.id)
  def environment = executionContext.localEnvironment
  def moleExecution: Option[MoleExecution] = Option(_moleExecution)
}
