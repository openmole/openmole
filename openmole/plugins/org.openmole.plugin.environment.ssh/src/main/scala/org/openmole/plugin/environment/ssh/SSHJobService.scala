package org.openmole.plugin.environment.ssh

import effectaside.Effect
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, SerializedJob, AccessControl }
import org.openmole.plugin.environment.batch.jobservice.JobServiceInterface
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, StorageInterface }
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import org.openmole.plugin.environment.ssh.SSHEnvironment.SSHJob
import org.openmole.tool.lock._

object SSHJobService {

  implicit def isJobService[A]: JobServiceInterface[SSHJobService[A]] = new JobServiceInterface[SSHJobService[A]] {
    override type J = SSHJob

    override def submit(env: SSHJobService[A], serializedJob: SerializedJob): J = env.register(serializedJob)
    override def state(env: SSHJobService[A], j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: SSHJobService[A], j: J): Unit = env.delete(j)
    override def stdOutErr(js: SSHJobService[A], j: SSHJob) = js.stdOutErr(j)

    override def accessControl(js: SSHJobService[A]): AccessControl = js.accessControl
  }
}

class SSHJobService[S](s: S, services: BatchEnvironment.Services, installation: RuntimeInstallation[_], env: SSHEnvironment[_], val accessControl: AccessControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], sshEffect: Effect[_root_.gridscale.ssh.SSH], systemEffect: Effect[effectaside.System]) {

  def register(serializedJob: SerializedJob) = {
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
      env.jobsStates.put(job, SSHEnvironment.Queued(jobDescription))
      job
    }
  }

  def submit(job: SSHEnvironment.SSHJob, description: gridscale.ssh.SSHJobDescription) =
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