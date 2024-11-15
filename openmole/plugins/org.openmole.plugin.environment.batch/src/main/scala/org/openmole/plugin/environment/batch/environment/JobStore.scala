package org.openmole.plugin.environment.batch.environment

import java.io.File
import org.openmole.core.context.Context
import org.openmole.core.serializer.SerializerService
import org.openmole.core.workflow.job.Job.Canceled
import org.openmole.core.workflow.job.{Job, JobGroup, RuntimeTask}
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.task.Task
import org.openmole.core.workspace.*
import org.openmole.tool.file.*
import org.openmole.tool.collection.*

object JobStore:

  def store(jobStore: JobStore, job: JobGroup)(implicit serializer: SerializerService): StoredJob =
    val storedMoleJobs: OneOrIArray[StoredMoleJob] =
      val moleJobs = JobGroup.moleJobs(job)
      if moleJobs.size == 1
      then store(jobStore, moleJobs.head)
      else moleJobs.map(mj ⇒ store(jobStore, mj))

    new StoredJob(JobGroup.moleExecution(job), storedMoleJobs)

  def store(jobStore: JobStore, moleJob: Job)(implicit serializer: SerializerService): StoredMoleJob =
    val f = jobStore.store.newFile("storedjob", ".bin")
    f.withOutputStream { os ⇒ serializer.serialize(moleJob.context, os) }
    new StoredMoleJob(
      f,
      moleJob.task,
      moleJob.id,
      moleJob.callBack)

  def load(storedJob: StoredJob)(implicit serializerService: SerializerService): JobGroup =
    val moleJobs = storedJob.storedMoleJobs.map(load)
    JobGroup(storedJob.moleExecution, moleJobs)

  def load(storedMoleJob: StoredMoleJob)(implicit serializerService: SerializerService): Job =
    val context = storedMoleJob.context.withInputStream { is ⇒ serializerService.deserialize[Context](is) }
    Job(
      task = storedMoleJob.task,
      context = context,
      id = storedMoleJob.id,
      callBack = storedMoleJob.callBack
    )

  def clean(job: StoredJob): Unit = job.storedMoleJobs.foreach(clean)
  def clean(job: StoredMoleJob): Unit = job.context.delete()

  class StoredJob(val moleExecution: MoleExecution, val storedMoleJobsValue: OneOrIArray[StoredMoleJob]):
    def storedMoleJobs = storedMoleJobsValue.toIArray

  class StoredMoleJob(
    val context:  File,
    val task:     RuntimeTask,
    val id:       Long,
    val callBack: Job.CallBack)

  def subMoleCanceled(storedMoleJob: StoredMoleJob) = storedMoleJob.callBack.subMoleCanceled()
  def finish(storedMoleJob: StoredMoleJob, result: Either[Context, Throwable]) = storedMoleJob.callBack.jobFinished(storedMoleJob.id, result)
  

case class JobStore(store: File)
