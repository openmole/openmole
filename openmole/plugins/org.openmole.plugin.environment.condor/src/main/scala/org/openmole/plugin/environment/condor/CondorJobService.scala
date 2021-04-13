package org.openmole.plugin.environment.condor

import gridscale.cluster.HeadNode
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }
import _root_.gridscale.effectaside

class CondorJobService[S, H](
  s:                 S,
  tmpDirectory:      String,
  installation:      RuntimeInstallation[_],
  parameters:        CondorEnvironment.Parameters,
  h:                 H,
  val accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], headNode: HeadNode[H], services: BatchEnvironment.Services, systemInterpreter: effectaside.Effect[effectaside.System]) {

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

    val description = _root_.gridscale.condor.CondorJobDescription(
      executable = "/bin/bash",
      arguments = remoteScript.content,
      workDirectory = jobDirectory,
      memory = parameters.memory,
      nodes = parameters.nodes,
      coreByNode = parameters.coresByNode orElse parameters.threads,
      requirements = parameters.requirements.map(_root_.gridscale.condor.CondorRequirement.apply)
    )

    accessControl { gridscale.condor.submit(h, description) }
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { GridScaleJobService.translateStatus(gridscale.condor.state(h, id)) }

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { gridscale.condor.clean(h, id) }

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    accessControl { (gridscale.condor.stdOut(h, id), gridscale.condor.stdErr(h, id)) }

}
