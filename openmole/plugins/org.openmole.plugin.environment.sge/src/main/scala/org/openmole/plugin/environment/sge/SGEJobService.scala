package org.openmole.plugin.environment.sge

import gridscale.cluster.HeadNode
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }

class SGEJobService[S](
  s:             S,
  tmpDirectory:  String,
  installation:  RuntimeInstallation[?],
  parameters:    SGEEnvironment.Parameters,
  headNode:      HeadNode,
  accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], services: BatchEnvironment.Services):

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
        parameters.threads,
        serializedJob,
        outputPath,
        s,
        modules = parameters.modules
      )

    val remoteScript = buildScript(serializedJob, outputPath)

    val description = _root_.gridscale.sge.SGEJobDescription(
      command = s"/bin/bash ${remoteScript.content}",
      queue = parameters.queue,
      workDirectory = jobDirectory,
      wallTime = parameters.wallTime,
      memory = parameters.memory
    )

    accessControl:
      gridscale.sge.submit(headNode, description)

  def state(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      GridScaleJobService.translateStatus(gridscale.sge.state(headNode, id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      gridscale.sge.clean(headNode, id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      (gridscale.sge.stdOut(headNode, id), gridscale.sge.stdErr(headNode, id))


