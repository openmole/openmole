package org.openmole.plugin.environment.oar

import gridscale.cluster.HeadNode
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }

class OARJobService[S](
  s:             S,
  tmpDirectory:  String,
  installation:  RuntimeInstallation[_],
  parameters:    OAREnvironment.Parameters,
  headNode:      HeadNode,
  accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], services: BatchEnvironment.Services) {

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

    val description = gridscale.oar.OARJobDescription(
      command = s"/bin/bash ${remoteScript.path}",
      workDirectory = jobDirectory,
      queue = parameters.queue,
      cpu = parameters.cpu,
      core = OAREnvironment.nbCores(parameters),
      wallTime = parameters.wallTime,
      bestEffort = parameters.bestEffort
    )

    accessControl { gridscale.oar.submit(headNode, description) }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl { GridScaleJobService.translateStatus(gridscale.oar.state(headNode, id)) }

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl { gridscale.oar.clean(headNode, id) }

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl { (gridscale.oar.stdOut(headNode, id), gridscale.oar.stdErr(headNode, id)) }

}
