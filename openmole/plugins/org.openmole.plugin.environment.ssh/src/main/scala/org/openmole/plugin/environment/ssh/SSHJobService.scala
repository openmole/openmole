package org.openmole.plugin.environment.ssh

import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.IUpdatable
import org.openmole.core.workflow.execution.Environment.ExceptionRaised
import org.openmole.core.workflow.execution.{Environment, ExecutionState}
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.environment.batch.environment.{AccessControl, BatchEnvironment, BatchExecutionJob, SerializedJob}
import org.openmole.plugin.environment.batch.storage.*
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.tool.logger.JavaLogger

import scala.ref.WeakReference

object SSHJobService extends JavaLogger:

  class Updater(environment: WeakReference[SSHEnvironment]) extends IUpdatable:
    var stop = false

    def update(): Boolean =
      if stop
      then false
      else environment.get match
        case Some(env) =>
          val sshJobIds = env.stateRegistry.submitted

          val runningJobResults = 
            AccessControl.defaultPrirority:
              env.accessControl:
                import env.ssh
                sshJobIds.toList.map { case (_, id) => util.Try(gridscale.ssh.SSHJobDescription.jobIsRunning(id)) }

          val errors = runningJobResults.collect { case util.Failure(x) => x }
          for e <- errors
          do env.error(ExceptionRaised(e, Log.WARNING))

          val boundNumberOfRunningJobs: Int = runningJobResults.map(_.getOrElse(true)).count(_ == true)
          val nbSubmit = env.slots - boundNumberOfRunningJobs
          val toSubmit = env.stateRegistry.queued.take(nbSubmit)

          for
            (job, desc, bj) ← toSubmit
          do
            try env.sshJobService.submit(job, desc, bj)
            catch
              case t: Throwable => env.error(Environment.ExecutionJobExceptionRaised(bj, t, SSHJobService.Log.WARNING))

          !stop

        case None => false


class SSHJobService[S](s: S, space: StorageSpace, services: BatchEnvironment.Services, env: SSHEnvironment, val accessControl: AccessControl)(using storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], ssh: _root_.gridscale.ssh.SSH, preference: Preference, tmpDirectory: TmpDirectory):

  lazy val installation = RuntimeInstallation(Frontend.ssh(env.host, env.port, env.timeout, env.authentication), s, space.baseDirectory)

  def register(batchExecutionJob: BatchExecutionJob, serializedJob: SerializedJob, outputPath: String, jobDirectory: String, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority

    val workDirectory = env.workDirectory getOrElse "/tmp"

    def buildScript(serializedJob: SerializedJob) =
      import services.*

      SharedStorage.buildScript(
        installation.apply,
        jobDirectory,
        workDirectory,
        env.openMOLEMemory,
        env.threads,
        serializedJob,
        outputPath,
        s,
        modules = env.modules,
        debug = env.debug
      )

    val remoteScript = buildScript(serializedJob)
    val jobDescription = gridscale.ssh.SSHJobDescription(
      command = s"/bin/bash ${remoteScript.content}",
      workDirectory = jobDirectory,
      timeout = env.killAfter
    )

    env.stateRegistry.registerJob(jobDescription, batchExecutionJob, remoteScript.jobWorkDirectory)

  def submit(job: SSHEnvironment.SSHJob, description: gridscale.ssh.SSHJobDescription, batchExecutionJob: BatchExecutionJob) =
    try
      val id = gridscale.ssh.submit(description)
      env.stateRegistry.update(job, SSHEnvironment.Submitted(id))
    catch
      case t: Throwable =>
        env.stateRegistry.update(job, SSHEnvironment.Failed)
        throw t


  def state(job: SSHEnvironment.SSHJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    env.stateRegistry.get(job) match
      case None                               => ExecutionState.DONE
      case Some(state: SSHEnvironment.Queued) => ExecutionState.SUBMITTED
      case Some(SSHEnvironment.Failed)        => ExecutionState.FAILED
      case Some(SSHEnvironment.Submitted(id)) =>
        accessControl:
          GridScaleJobService.translateStatus(gridscale.ssh.state(id))

  def delete(job: SSHEnvironment.SSHJob, priority: AccessControl.Priority): Unit =
    given AccessControl.Priority = priority
    val jobState = env.stateRegistry.remove(job)
    jobState match
      case Some(SSHEnvironment.Submitted(id)) => accessControl:
        gridscale.ssh.clean(id)
        gridscale.ssh.run(s"rm -rf ${job.workDirectory}")
      case _ =>

  def stdOutErr(j: SSHEnvironment.SSHJob, priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    val jobState = env.stateRegistry.get(j)
    jobState match
      case Some(SSHEnvironment.Submitted(id)) => accessControl { (gridscale.ssh.stdOut(id), gridscale.ssh.stdErr(id)) }
      case _                                  => ("", "")