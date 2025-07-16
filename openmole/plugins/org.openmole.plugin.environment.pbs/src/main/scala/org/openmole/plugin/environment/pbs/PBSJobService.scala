package org.openmole.plugin.environment.pbs

import gridscale.cluster.HeadNode
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, BatchExecutionJob, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.pbs.PBSEnvironment.Parameters
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }

class PBSJobService[S](
  s:                 S,
  tmpDirectory:      String,
  installation:      RuntimeInstallation[?],
  parameters:        Parameters,
  headnode:          HeadNode,
  val accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], services: BatchEnvironment.Services):

  import services._

  def submit(serializedJob: SerializedJob, outputPath: String, jobDirectory: String, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority

    val workDirectory = parameters.workDirectory getOrElse "/tmp"

    def buildScript(serializedJob: SerializedJob, outputPath: String) =
      SharedStorage.buildScript(
        installation.apply,
        jobDirectory,
        workDirectory,
        parameters.openMOLEMemory,
        serializedJob,
        outputPath,
        s,
        modules = parameters.modules
      )

    val remoteScript = buildScript(serializedJob, outputPath)

    val description = gridscale.pbs.PBSJobDescription(
      command = s"/bin/bash ${remoteScript.path}",
      workDirectory = jobDirectory,
      queue = parameters.queue,
      wallTime = parameters.wallTime,
      memory = parameters.memory,
      nodes = parameters.nodes,
      coreByNode = parameters.coreByNode orElse parameters.runtimeSetting.flatMap(_.threads),
      flavour = parameters.flavour
    )

    import PBSEnvironment.Log._

    log(FINE, s"""Submitting PBS job, PBS script:
                 |${gridscale.pbs.impl.toScript(description)("uniqId")}
                 |bash script:
                 |$remoteScript""".stripMargin)

    val id = accessControl { gridscale.pbs.submit(headnode, description) }

    log(FINE, s"""Submitted PBS job with PBS script:
                 |uniqId: ${id.uniqId}
                 |job id: ${id.jobId}""".stripMargin)

    id

  def state(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      GridScaleJobService.translateStatus(gridscale.pbs.state(headnode, id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      gridscale.pbs.clean(headnode, id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      (gridscale.pbs.stdOut(headnode, id), gridscale.pbs.stdErr(headnode, id))
