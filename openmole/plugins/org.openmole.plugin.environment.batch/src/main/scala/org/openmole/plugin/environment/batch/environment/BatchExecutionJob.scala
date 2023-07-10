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

import org.openmole.core.compiler.CompilationContext
import org.openmole.core.communication.message.RunnableTask
import org.openmole.core.fileservice.FileService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.serializer.{ PluginAndFilesListing, SerializerService }
import org.openmole.core.workflow.execution.{ ExecutionJob, ExecutionState }
import org.openmole.core.workflow.job.JobGroup
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.REPLClassCache
import org.openmole.plugin.environment.batch.environment.JobStore.StoredJob
import org.openmole.tool.bytecode.listAllClasses
import org.openmole.tool.file._
import org.openmole.tool.osgi.{ ClassFile, VersionedPackage, createBundle }

import java.io.File
import java.nio.file.Files
import java.util.UUID
import java.net.URLClassLoader

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

  def replClassesToPlugins(classDirectory: File, classLoader: ClassLoader)(implicit newFile: TmpDirectory, fileService: FileService) = {
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

    def bundleFile(closures: ClosuresBundle): Option[File] = 
      if closures.classes.isEmpty then None
      else
        val bundle = newFile.newFile("closureBundle", ".jar")
        try createBundle("closure-" + UUID.randomUUID.toString, "1.0", closures.classes, closures.exported, closures.dependencies, bundle)
        catch {
          case e: Throwable ⇒
            bundle.delete()
            throw e
        }
        Some(fileService.wrapRemoveOnGC(bundle))
    
    val closuresBundle = bundle(classDirectory, classLoader)
    (closuresBundle, bundleFile(closuresBundle))
  }

  def apply(id: Long, job: JobGroup, jobStore: JobStore)(implicit serializerService: SerializerService, tmpDirectory: TmpDirectory, fileService: FileService) = {
    val pluginsAndFiles = serializerService.pluginsAndFiles(JobGroup.moleJobs(job).map(RunnableTask(_)))
    val plugins = pluginsAndFiles.plugins.distinctBy(_.getCanonicalPath)
    val storedJob = JobStore.store(jobStore, job)
    new BatchExecutionJob(id, storedJob, IArray(pluginsAndFiles.files*), IArray(plugins*))
  }
}

class BatchExecutionJob(
  val id: Long,
  val storedJob: StoredJob,
  val files: IArray[File],
  val plugins: IArray[File]) extends ExecutionJob { bej ⇒

  def moleJobIds = storedJob.storedMoleJobs.map(_.id)
  private def job(implicit serializerService: SerializerService) = JobStore.load(storedJob)
  def runnableTasks(implicit serializerService: SerializerService) = JobGroup.moleJobs(job).map(RunnableTask(_))

  private[environment] var _state: ExecutionState = ExecutionState.READY

  def state = _state

}