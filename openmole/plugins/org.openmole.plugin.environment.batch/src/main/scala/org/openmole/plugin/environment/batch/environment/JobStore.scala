package org.openmole.plugin.environment.batch.environment

import java.io.File

import org.openmole.core.context.Context
import org.openmole.core.serializer.SerializerService
import org.openmole.core.workflow.job.MoleJob.Canceled
import org.openmole.core.workflow.job.{ Job, MoleJob }
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.task.Task
import org.openmole.core.workspace._
import org.openmole.tool.file._

object JobStore {

  def store(jobStore: JobStore, job: Job)(implicit serializer: SerializerService): StoredJob = {
    val moleJobs = Job.moleJobs(job)
    new StoredJob(Job.moleExecution(job), moleJobs.toArray.map(mj ⇒ store(jobStore, mj)))
  }

  def store(jobStore: JobStore, moleJob: MoleJob)(implicit serializer: SerializerService): StoredMoleJob = {
    val f = TmpDirectory(jobStore.store).newFile("storedjob", ".bin.gz")
    f.withGzippedOutputStream { os ⇒ serializer.serialize(moleJob.context, os) }
    new StoredMoleJob(
      f,
      moleJob.task,
      moleJob.id,
      moleJob.jobFinished,
      moleJob.subMoleCanceled)
  }

  def load(storedJob: StoredJob)(implicit serializerService: SerializerService): Job = {
    val moleJobs = storedJob.storedMoleJobs.map(load)
    Job(storedJob.moleExecution, moleJobs)
  }

  def load(storedMoleJob: StoredMoleJob)(implicit serializerService: SerializerService): MoleJob = {
    val context = storedMoleJob.context.withGzippedInputStream { is ⇒ serializerService.deserialize[Context](is) }
    MoleJob(
      task = storedMoleJob.task,
      context = context,
      id = storedMoleJob.id,
      jobFinished = storedMoleJob.jobFinished,
      subMoleCanceled = storedMoleJob.subMoleCanceled
    )
  }

  def clean(job: StoredJob): Unit = job.storedMoleJobs.foreach(clean)
  def clean(job: StoredMoleJob): Unit = job.context.delete()

  class StoredJob(val moleExecution: MoleExecution, val storedMoleJobs: Array[StoredMoleJob])
  class StoredMoleJob(
    val context:         File,
    val task:            Task,
    val id:              Long,
    val jobFinished:     MoleJob.JobFinished,
    val subMoleCanceled: Canceled) {
  }

  def finish(storedMoleJob: StoredMoleJob, result: Either[Context, Throwable]) = storedMoleJob.jobFinished(storedMoleJob.id, result)

}

case class JobStore(store: File)
