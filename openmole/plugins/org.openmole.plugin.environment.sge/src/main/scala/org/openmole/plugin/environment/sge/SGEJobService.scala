package org.openmole.plugin.environment.sge

import gridscale.cluster.HeadNode
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }
import _root_.gridscale.effectaside

class SGEJobService[S, H](
  s:             S,
  tmpDirectory:  String,
  installation:  RuntimeInstallation[_],
  parameters:    SGEEnvironment.Parameters,
  h:             H,
  accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], headNode: HeadNode[H], services: BatchEnvironment.Services, systemInterpreter: effectaside.Effect[effectaside.System]) {

  import services._

  def submit(serializedJob: SerializedJob, outputPath: String, jobDirectory: String) = {
    val workDirectory = parameters.workDirectory getOrElse "/tmp"

    def buildScript(serializedJob: SerializedJob, outputPath: String) = {
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
    }

    val remoteScript = buildScript(serializedJob, outputPath)

    val description = _root_.gridscale.sge.SGEJobDescription(
      command = s"/bin/bash $remoteScript",
      queue = parameters.queue,
      workDirectory = jobDirectory,
      wallTime = parameters.wallTime,
      memory = parameters.memory
    )

    accessControl { gridscale.sge.submit(h, description) }
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { GridScaleJobService.translateStatus(gridscale.sge.state(h, id)) }

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { gridscale.sge.clean(h, id) }

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { (gridscale.sge.stdOut(h, id), gridscale.sge.stdErr(h, id)) }

}
