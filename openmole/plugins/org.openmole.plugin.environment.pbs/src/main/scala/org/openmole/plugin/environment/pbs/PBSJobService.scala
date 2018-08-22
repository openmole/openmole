package org.openmole.plugin.environment.pbs

import gridscale.cluster.HeadNode
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, BatchExecutionJob, SerializedJob }
import org.openmole.plugin.environment.batch.jobservice.JobServiceInterface
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.pbs.PBSEnvironment.Parameters
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }

object PBSJobService {

  implicit def isJobService[A, B]: JobServiceInterface[PBSJobService[A, B]] = new JobServiceInterface[PBSJobService[A, B]] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: PBSJobService[A, B], serializedJob: SerializedJob, batchExecutionJob: BatchExecutionJob): J = env.submit(serializedJob)
    override def state(env: PBSJobService[A, B], j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: PBSJobService[A, B], j: J): Unit = env.delete(j)
    override def stdOutErr(js: PBSJobService[A, B], j: J) = js.stdOutErr(j)
    override def accessControl(js: PBSJobService[A, B]): AccessControl = js.accessControl
  }

}

class PBSJobService[S, H](
  s:                 S,
  installation:      RuntimeInstallation[_],
  parameters:        Parameters,
  h:                 H,
  val accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], headNode: HeadNode[H], services: BatchEnvironment.Services) {

  import services._
  implicit val systemInterpreter = effectaside.System()

  def submit(serializedJob: SerializedJob) = {
    def buildScript(serializedJob: SerializedJob) = {
      SharedStorage.buildScript(
        installation.apply,
        parameters.workDirectory,
        parameters.openMOLEMemory,
        parameters.threads,
        serializedJob,
        s
      )
    }

    val (remoteScript, workDirectory) = buildScript(serializedJob)

    val description = gridscale.pbs.PBSJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = workDirectory,
      queue = parameters.queue,
      wallTime = parameters.wallTime,
      memory = Some(BatchEnvironment.requiredMemory(parameters.openMOLEMemory, parameters.memory)),
      nodes = parameters.nodes,
      coreByNode = parameters.coreByNode orElse parameters.threads,
      flavour = parameters.flavour
    )

    import PBSEnvironment.Log._

    log(FINE, s"""Submitting PBS job, PBS script:
                 |${gridscale.pbs.impl.toScript(description)("uniqId")}
                 |bash script:
                 |$remoteScript""".stripMargin)

    val id = accessControl { gridscale.pbs.submit(h, description) }

    log(FINE, s"""Submitted PBS job with PBS script:
                 |uniqId: ${id.uniqId}
                 |job id: ${id.jobId}""".stripMargin)

    id
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { GridScaleJobService.translateStatus(gridscale.pbs.state(h, id)) }

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { gridscale.pbs.clean(h, id) }

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { (gridscale.pbs.stdOut(h, id), gridscale.pbs.stdErr(h, id)) }

}