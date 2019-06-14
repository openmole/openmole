package org.openmole.plugin.environment.pbs

import gridscale.cluster.HeadNode
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, BatchExecutionJob, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.pbs.PBSEnvironment.Parameters
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }

class PBSJobService[S, H](
  s:                 S,
  tmpDirectory:      String,
  installation:      RuntimeInstallation[_],
  parameters:        Parameters,
  h:                 H,
  val accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], headNode: HeadNode[H], services: BatchEnvironment.Services, systemInterpreter: effectaside.Effect[effectaside.System]) {

  import services._

  def submit(serializedJob: SerializedJob, outputPath: String, jobDirectory: String) = {
    val workDirectory = parameters.workDirectory getOrElse jobDirectory

    def buildScript(serializedJob: SerializedJob, outputPath: String) = {
      SharedStorage.buildScript(
        installation.apply,
        jobDirectory,
        workDirectory,
        parameters.openMOLEMemory,
        parameters.threads,
        serializedJob,
        outputPath,
        s
      )
    }

    val remoteScript = buildScript(serializedJob, outputPath)

    val description = gridscale.pbs.PBSJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = jobDirectory,
      queue = parameters.queue,
      wallTime = parameters.wallTime,
      memory = parameters.memory,
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