package org.openmole.plugin.environment.condor

import gridscale.cluster.HeadNode
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }

class CondorJobService[S](
  s:                 S,
  tmpDirectory:      String,
  installation:      RuntimeInstallation[_],
  parameters:        CondorEnvironment.Parameters,
  headNode:          HeadNode,
  val accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], services: BatchEnvironment.Services):

  import services.*

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

    val description = _root_.gridscale.condor.CondorJobDescription(
      executable = "/bin/bash",
      arguments = remoteScript.path,
      workDirectory = jobDirectory,
      memory = parameters.memory,
      nodes = parameters.nodes,
      coreByNode = parameters.coresByNode orElse parameters.runtimeSetting.flatMap(_.threads),
      requirements = parameters.requirements.map(_root_.gridscale.condor.CondorRequirement.apply)
    )

    accessControl { gridscale.condor.submit(headNode, description) }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl { GridScaleJobService.translateStatus(gridscale.condor.state(headNode, id)) }

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl { gridscale.condor.clean(headNode, id) }

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl { (gridscale.condor.stdOut(headNode, id), gridscale.condor.stdErr(headNode, id)) }


