package org.openmole.plugin.environment.slurm

import gridscale.cluster.HeadNode
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.{ RuntimeInstallation, SharedStorage }

class SLURMJobService[S](
  s:                 S,
  tmpDirectory:      String,
  installation:      RuntimeInstallation[?],
  parameters:        SLURMEnvironment.Parameters,
  h:                 HeadNode,
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
        debug = parameters.debug,
        modules = parameters.modules
      )

    val remoteScript = buildScript(serializedJob, outputPath)

    val description = _root_.gridscale.slurm.SLURMJobDescription(
      command = s"/bin/bash ${remoteScript.path}",
      partition = parameters.partition,
      workDirectory = jobDirectory,
      time = parameters.time,
      memory = parameters.memory,
      nodes = parameters.nodes,
      ntasks = parameters.nTasks,
      cpuPerTask = parameters.cpuPerTask orElse parameters.runtimeSetting.flatMap(_.threads),
      qos = parameters.qos,
      gres = parameters.gres.toList,
      constraints = parameters.constraints.toList,
      reservation = parameters.reservation,
      wckey = parameters.wckey,
      exclusive = parameters.exclusive
    )

    accessControl { gridscale.slurm.submit(h, description) }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      GridScaleJobService.translateStatus(gridscale.slurm.state(h, id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      gridscale.slurm.clean(h, id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    accessControl:
      (gridscale.slurm.stdOut(h, id), gridscale.slurm.stdErr(h, id))


