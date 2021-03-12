package org.openmole.plugin.environment.ssh

import _root_.gridscale.effectaside
import org.openmole.core.threadprovider.IUpdatable
import org.openmole.core.workflow.execution.Environment.ExceptionRaised
import org.openmole.core.workflow.execution.{ Environment, ExecutionState }
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment, BatchExecutionJob, SerializedJob }
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.tool.logger.JavaLogger

import scala.ref.WeakReference

object SSHJobService extends JavaLogger {

  class Updater(environment: WeakReference[SSHEnvironment[_]]) extends IUpdatable {

    var stop = false

    def update() =
      if (stop) false
      else environment.get match {
        case Some(env) ⇒
          val sshJobIds = env.stateRegistry.submitted

          val runningJobResults = env.accessControl {
            import env.sshInterpreter
            sshJobIds.toList.map { case (_, id) ⇒ util.Try(gridscale.ssh.SSHJobDescription.jobIsRunning(env.sshServer, id)) }
          }

          val errors = runningJobResults.collect { case util.Failure(x) ⇒ x }
          for {
            e ← errors
          } env.error(ExceptionRaised(e, Log.WARNING))

          val boundNumberOfRunningJobs: Int = runningJobResults.map(_.getOrElse(true)).count(_ == true)
          val nbSubmit = env.slots - boundNumberOfRunningJobs
          val toSubmit = env.stateRegistry.queued.take(nbSubmit)

          for {
            (job, desc, bj) ← toSubmit
          } try env.sshJobService.submit(job, desc, bj)
          catch {
            case t: Throwable ⇒ env.error(Environment.ExecutionJobExceptionRaised(bj, t, SSHJobService.Log.WARNING))
          }

          !stop

        case None ⇒ false
      }
  }

}

class SSHJobService[S](s: S, tmpDirectory: String, services: BatchEnvironment.Services, installation: RuntimeInstallation[_], env: SSHEnvironment[_], val accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], sshEffect: effectaside.Effect[_root_.gridscale.ssh.SSH], systemEffect: effectaside.Effect[effectaside.System]) {

  def register(batchExecutionJob: BatchExecutionJob, serializedJob: SerializedJob, outputPath: String, jobDirectory: String) = {

    val workDirectory = env.workDirectory getOrElse "/tmp"

    def buildScript(serializedJob: SerializedJob) = {
      import services._

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
    }

    val remoteScript = buildScript(serializedJob)
    val jobDescription = gridscale.ssh.SSHJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = jobDirectory
    )

    env.stateRegistry.registerJob(jobDescription, batchExecutionJob)
  }

  def submit(job: SSHEnvironment.SSHJob, description: gridscale.ssh.SSHJobDescription, batchExecutionJob: BatchExecutionJob) =
    try {
      val id = gridscale.ssh.submit(env.sshServer, description)
      env.stateRegistry.update(job, SSHEnvironment.Submitted(id))
    }
    catch {
      case t: Throwable ⇒
        env.stateRegistry.update(job, SSHEnvironment.Failed)
        throw t
    }

  def state(job: SSHEnvironment.SSHJob) =
    env.stateRegistry.get(job) match {
      case None                               ⇒ ExecutionState.DONE
      case Some(state: SSHEnvironment.Queued) ⇒ ExecutionState.SUBMITTED
      case Some(SSHEnvironment.Failed)        ⇒ ExecutionState.FAILED
      case Some(SSHEnvironment.Submitted(id)) ⇒ accessControl { GridScaleJobService.translateStatus(gridscale.ssh.state(env.sshServer, id)) }
    }

  def delete(job: SSHEnvironment.SSHJob): Unit = {
    val jobState = env.stateRegistry.remove(job)
    jobState match {
      case Some(SSHEnvironment.Submitted(id)) ⇒ accessControl { gridscale.ssh.clean(env.sshServer, id) }
      case _                                  ⇒
    }
  }

  def stdOutErr(j: SSHEnvironment.SSHJob) = {
    val jobState = env.stateRegistry.get(j)
    jobState match {
      case Some(SSHEnvironment.Submitted(id)) ⇒ accessControl { (gridscale.ssh.stdOut(env.sshServer, id), gridscale.ssh.stdErr(env.sshServer, id)) }
      case _                                  ⇒ ("", "")
    }
  }

}