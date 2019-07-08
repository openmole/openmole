package org.openmole.plugin.environment.oar

import gridscale.cluster.HeadNode
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }

class OARJobService[S, H](
  s:             S,
  tmpDirectory:  String,
  installation:  RuntimeInstallation[_],
  parameters:    OAREnvironment.Parameters,
  h:             H,
  accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], headNode: HeadNode[H], services: BatchEnvironment.Services, systemInterpreter: effectaside.Effect[effectaside.System]) {

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

    val description = gridscale.oar.OARJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = jobDirectory,
      queue = parameters.queue,
      cpu = parameters.cpu,
      core = OAREnvironment.nbCores(parameters),
      wallTime = parameters.wallTime,
      bestEffort = parameters.bestEffort
    )

    accessControl { gridscale.oar.submit(h, description) }
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { GridScaleJobService.translateStatus(gridscale.oar.state(h, id)) }

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { gridscale.oar.clean(h, id) }

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { (gridscale.oar.stdOut(h, id), gridscale.oar.stdErr(h, id)) }

}
