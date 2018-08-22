package org.openmole.plugin.environment.ssh

import effectaside.Effect
import org.openmole.core.threadprovider.IUpdatable
import org.openmole.core.workflow.execution.Environment.ExceptionRaised
import org.openmole.core.workflow.execution.{Environment, ExecutionState}
import org.openmole.plugin.environment.batch.environment.{AccessControl, BatchEnvironment, BatchExecutionJob, SerializedJob}
import org.openmole.plugin.environment.batch.jobservice.JobServiceInterface
import org.openmole.plugin.environment.batch.storage.{HierarchicalStorageInterface, StorageInterface}
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.SSHEnvironment.{Queued, SSHJob}
import org.openmole.tool.lock._
import org.openmole.tool.logger.JavaLogger

import scala.ref.WeakReference

object SSHJobService extends JavaLogger {

  implicit def isJobService[A]: JobServiceInterface[SSHJobService[A]] = new JobServiceInterface[SSHJobService[A]] {
    override type J = SSHJob

    override def submit(js: SSHJobService[A], serializedJob: SerializedJob, batchExecutionJob: BatchExecutionJob): J = js.accessControl { js.register(serializedJob, batchExecutionJob) }
    override def state(js: SSHJobService[A], j: J): ExecutionState.ExecutionState = js.accessControl { js.state(j) }
    override def delete(js: SSHJobService[A], j: J): Unit = js.accessControl { js.delete(j) }
    override def stdOutErr(js: SSHJobService[A], j: SSHJob) = js.accessControl { js.stdOutErr(j) }

    override def accessControl(js: SSHJobService[A]): AccessControl = js.accessControl
  }

  class Updater(environment: WeakReference[SSHEnvironment[_]]) extends IUpdatable {

    var stop = false

    def update() =
      if (stop) false
      else environment.get match {
        case Some(env) ⇒
          val sshJobIds = env.queuesLock { env.jobsStates.toSeq.collect { case (j, SSHEnvironment.Submitted(id)) ⇒ id } }

          val runningJobResults = env.accessControl {
            import env.sshInterpreter
            sshJobIds.toList.map(id ⇒ util.Try(gridscale.ssh.SSHJobDescription.jobIsRunning(env.sshServer, id)))
          }

          val errors =  runningJobResults.collect { case util.Failure(x) => x }
          for {
            e <- errors
          } env.error(ExceptionRaised(e, Log.WARNING))

          val boundNumberOfRunningJobs: Int = runningJobResults.map(_.getOrElse(true)).count(_ == true)

          val nbSubmit = env.slots - boundNumberOfRunningJobs
          val toSubmit = env.queuesLock {
            env.jobsStates.collect { case (job, Queued(desc, bj)) ⇒ (job, desc, bj) }.take(nbSubmit)
          }

          for {
            (job, desc, bj) ← toSubmit
          } try env.sshJobService.submit(job, desc, bj)
            catch {
              case t: Throwable => env.error(Environment.ExecutionJobExceptionRaised(bj, t, SSHJobService.Log.WARNING))
            }

          !stop

        case None ⇒ false
      }
  }
}

class SSHJobService[S](s: S, services: BatchEnvironment.Services, installation: RuntimeInstallation[_], env: SSHEnvironment[_], val accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], sshEffect: Effect[_root_.gridscale.ssh.SSH], systemEffect: Effect[effectaside.System]) {

  def register(serializedJob: SerializedJob, batchExecutionJob: BatchExecutionJob) = {
    def buildScript(serializedJob: SerializedJob) = {
      import services._
      SharedStorage.buildScript(
        installation.apply,
        env.workDirectory,
        env.openMOLEMemory,
        env.threads,
        serializedJob,
        s
      )
    }

    val (remoteScript, workDirectory) = buildScript(serializedJob)
    val jobDescription = gridscale.ssh.SSHJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = workDirectory
    )

    env.queuesLock {
      val job = SSHEnvironment.SSHJob(env.jobId.getAndIncrement())
      env.jobsStates.put(job, SSHEnvironment.Queued(jobDescription, batchExecutionJob))
      job
    }
  }

  def submit(job: SSHEnvironment.SSHJob, description: gridscale.ssh.SSHJobDescription, batchExecutionJob: BatchExecutionJob) =
    try {
      val id = gridscale.ssh.submit(env.sshServer, description)
      env.queuesLock { env.jobsStates.put(job, SSHEnvironment.Submitted(id)) }
    }
    catch {
      case t: Throwable ⇒
        env.queuesLock { env.jobsStates.put(job, SSHEnvironment.Failed) }
        throw t
    }

  def state(job: SSHEnvironment.SSHJob) = env.queuesLock {
    env.jobsStates.get(job) match {
      case None                               ⇒ ExecutionState.DONE
      case Some(state: SSHEnvironment.Queued) ⇒ ExecutionState.SUBMITTED
      case Some(SSHEnvironment.Failed)        ⇒ ExecutionState.FAILED
      case Some(SSHEnvironment.Submitted(id)) ⇒ GridScaleJobService.translateStatus(gridscale.ssh.state(env.sshServer, id))
    }
  }

  def delete(job: SSHEnvironment.SSHJob): Unit = {
    val jobState = env.queuesLock { env.jobsStates.remove(job) }
    jobState match {
      case Some(SSHEnvironment.Submitted(id)) ⇒ gridscale.ssh.clean(env.sshServer, id)
      case _                                  ⇒
    }
  }

  def stdOutErr(j: SSHEnvironment.SSHJob) = {
    val jobState = env.queuesLock { env.jobsStates.get(j) }
    jobState match {
      case Some(SSHEnvironment.Submitted(id)) ⇒ (gridscale.ssh.stdOut(env.sshServer, id), gridscale.ssh.stdErr(env.sshServer, id))
      case _                                  ⇒ ("", "")
    }
  }

}