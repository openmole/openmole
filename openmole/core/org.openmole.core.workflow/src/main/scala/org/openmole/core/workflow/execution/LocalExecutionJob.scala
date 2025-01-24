package org.openmole.core.workflow.execution

import org.openmole.core.workflow.job.Job
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.tool.collection.*

/**
 * An [[ExecutionJob]] on the local environment (retrieved from the [[TaskExecutionContext]])
 *
 * @param executionContext
 * @param moleExecution
 */
object LocalExecutionJob:

  def apply(
    id:               Long,
    executionContext: TaskExecutionContext.Partial,
    jobs:             Job | IArray[Job],
    moleExecution:    Option[MoleExecution]) =
    new LocalExecutionJob(
      id = id,
      executionContext = executionContext,
      _jobs = jobs,
      _moleExecution = moleExecution.orNull)


case class LocalExecutionJob(id: Long, executionContext: TaskExecutionContext.Partial, _jobs: OneOrIArray[Job], _moleExecution: MoleExecution) extends ExecutionJob:
  def jobs: IArray[Job] = _jobs.toIArray
  def moleJobIds = jobs.map(_.id)
  def moleExecution: Option[MoleExecution] = Option(_moleExecution)
