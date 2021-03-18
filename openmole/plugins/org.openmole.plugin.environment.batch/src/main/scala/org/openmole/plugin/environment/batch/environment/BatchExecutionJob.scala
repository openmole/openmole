package org.openmole.plugin.environment.batch.environment

/*
 * Copyright (C) 2019 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File
import java.nio.file.Files
import java.util.UUID
import org.openmole.core.communication.message.RunnableTask
import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.FileService
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.serializer.{ PluginAndFilesListing, SerializerService }
import org.openmole.core.workflow.execution.{ Environment, ExecutionJob }
import org.openmole.core.workflow.execution.ExecutionState.{ DONE, ExecutionState, FAILED, KILLED, READY }
import org.openmole.core.workflow.job.Job
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.environment.batch.environment.JobStore.StoredJob
import org.openmole.tool.bytecode.listAllClasses
import org.openmole.tool.osgi.{ ClassFile, VersionedPackage, createBundle }
import org.openmole.tool.file._

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

object BatchExecutionJob {

  def toClassPath(c: String) = s"${c.replace('.', '/')}.class"
  def toClassName(p: String) = p.dropRight(".class".size).replace("/", ".")

  def replClassDirectory(c: Class[_]) = {
    val replClassloader = c.getClassLoader.asInstanceOf[URLClassLoader]
    val location = toClassPath(c.getName)
    val classURL = replClassloader.findResource(location)
    new File(classURL.getPath.dropRight(location.size))
  }

  def allClasses(directory: File): Seq[ClassFile] = {
    import java.nio.file._

    import collection.JavaConverters._
    Files.walk(directory.toPath).
      filter(p ⇒ Files.isRegularFile(p) && p.toFile.getName.endsWith(".class")).iterator().asScala.
      map { p ⇒
        val path = directory.toPath.relativize(p)
        ClassFile(path.toString, p.toFile)
      }.toList
  }

  case class ClosuresBundle(classes: Seq[ClassFile], exported: Seq[String], dependencies: Seq[VersionedPackage], plugins: Seq[File])

  def replClassesToPlugins(replClasses: Seq[Class[_]])(implicit newFile: TmpDirectory, fileService: FileService) = {
    val replDirectories = replClasses.map(c ⇒ c.getClassLoader -> BatchExecutionJob.replClassDirectory(c)).distinct

    def bundle(directory: File, classLoader: ClassLoader) = {
      val allClassFiles = BatchExecutionJob.allClasses(directory)

      val mentionedClasses =
        for {
          f ← allClassFiles.toList
          t ← listAllClasses(Files.readAllBytes(f.file))
          c ← util.Try[Class[_]](Class.forName(t.getClassName, false, classLoader)).toOption.toSeq
        } yield c

      def toVersionedPackage(c: Class[_]) = {
        val p = c.getName.reverse.dropWhile(_ != '.').drop(1).reverse
        PluginManager.bundleForClass(c).map { b ⇒ VersionedPackage(p, Some(b.getVersion.toString)) }
      }

      val packages = mentionedClasses.flatMap(toVersionedPackage).distinct
      val plugins = mentionedClasses.flatMap(PluginManager.pluginsForClass)

      val exported =
        allClassFiles.flatMap(c ⇒ Option(new File(c.path).getParent)).distinct.
          filter(PluginAndFilesListing.looksLikeREPLClassName).
          map(_.replace("/", "."))

      val replClassFiles = allClassFiles.filter(c ⇒ PluginAndFilesListing.looksLikeREPLClassName(c.path.replace("/", ".")))

      BatchExecutionJob.ClosuresBundle(replClassFiles, exported, packages, plugins)
    }

    def bundleFile(closures: ClosuresBundle) = {
      val bundle = newFile.newFile("closureBundle", ".jar")
      try createBundle("closure-" + UUID.randomUUID.toString, "1.0", closures.classes, closures.exported, closures.dependencies, bundle)
      catch {
        case e: Throwable ⇒
          bundle.delete()
          throw e
      }
      fileService.wrapRemoveOnGC(bundle)
    }

    val (bfs, plugins) =
      replDirectories.map {
        case (c, d) ⇒
          val b = bundle(d, c)
          (bundleFile(b), b.plugins)
      }.unzip

    bfs ++ plugins.flatten.toList.distinct
  }

  def apply(job: Job, environment: BatchEnvironment)(implicit serializerService: SerializerService, tmpDirectory: TmpDirectory, fileService: FileService) = {
    val pluginsAndFiles = serializerService.pluginsAndFiles(Job.moleJobs(job).map(RunnableTask(_)))

    def closureBundleAndPlugins = {
      val replClasses = pluginsAndFiles.replClasses
      environment.relpClassesCache.cache(Job.moleExecution(job), pluginsAndFiles.replClasses.map(_.getName).toSet, preCompute = false) { _ ⇒
        BatchExecutionJob.replClassesToPlugins(replClasses)
      }
    }

    val plugins = pluginsAndFiles.plugins ++ closureBundleAndPlugins
    val storedJob = JobStore.store(environment.jobStore, job)

    new BatchExecutionJob(storedJob, environment, pluginsAndFiles.files, plugins)
  }
}

class BatchExecutionJob(val storedJob: StoredJob, val environment: BatchEnvironment, val files: Seq[File], val plugins: Seq[File]) extends ExecutionJob { bej ⇒

  import environment.services._

  def moleJobIds = storedJob.storedMoleJobs.map(_.id)
  private def job = JobStore.load(storedJob)
  def runnableTasks = Job.moleJobs(job).map(RunnableTask(_))

  private var _state: ExecutionState = READY

  def state = _state

  def state_=(newState: ExecutionState)(implicit eventDispatcher: EventDispatcher) = synchronized {
    if (state != KILLED && newState != state) {
      newState match {
        case DONE ⇒ environment._done.incrementAndGet()
        case FAILED ⇒
          if (state == DONE) environment._done.decrementAndGet()
          environment._failed.incrementAndGet()
        case _ ⇒
      }

      eventDispatcher.trigger(environment, Environment.JobStateChanged(this, newState, this.state))
      _state = newState
    }
  }

}