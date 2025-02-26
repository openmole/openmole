package org.openmole.plugin.environment.batch.storage

import java.nio.file.spi.FileTypeDetector

import org.openmole.core.preference.{ PreferenceLocation, Preference }
import org.openmole.core.replication.ReplicaCatalog
import java.util.regex.Pattern

import scala.util._
import squants.time.TimeConversions._
import gridscale._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.outputmanager.OutputManager
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment }
import org.openmole.plugin.environment.batch.refresh.{ JobManager, RetryAction }
import org.openmole.tool.cache.Lazy
import org.openmole.tool.logger.JavaLogger
import squants._

object StorageSpace:

  def lastBegining(interval: Time) =
    val modulo = interval.toMillis
    val time = System.currentTimeMillis()
    val sinceBeginingOfTheDay = time % modulo
    (time - sinceBeginingOfTheDay).toString

  def timedUniqName = org.openmole.tool.file.uniqName(System.currentTimeMillis.toString, "", separator = "_")



object HierarchicalStorageSpace extends JavaLogger:
  val TmpDirRemoval = PreferenceLocation("StorageService", "TmpDirRemoval", Some(30 days))
  val TmpDirCreation = PreferenceLocation("StorageService", "TmpDirCreation", Some(1 hours))

  def create[S](s: S, root: String, storageId: String, isConnectionError: Throwable => Boolean)(using storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], preference: Preference, priority: AccessControl.Priority) =
    val persistent = "persistent/"
    val tmp = "tmp/"

    val baseDirectory =
      try createBasePath(s, root, isConnectionError)
      catch
        case e: Throwable => throw new InternalProcessingError(s"Error creating base directory $root on storage $s", e)

    val replicaDirectory = hierarchicalStorageInterface.child(s, baseDirectory, persistent)

    try
      if !storageInterface.exists(s, replicaDirectory)
      then hierarchicalStorageInterface.makeDir(s, replicaDirectory)
    catch
      case e: Throwable => throw new InternalProcessingError(s"Error creating replica directory $replicaDirectory on storage $s", e)

    val tmpDirectory = hierarchicalStorageInterface.child(s, baseDirectory, tmp)

    try
      if !storageInterface.exists(s, tmpDirectory)
      then hierarchicalStorageInterface.makeDir(s, tmpDirectory)
    catch
      case e: Throwable => throw new InternalProcessingError(s"Error creating tmp directory $tmpDirectory on storage $s", e)


    StorageSpace(baseDirectory, replicaDirectory, tmpDirectory)

  def clean[S](s: S, storageSpace: StorageSpace, background: Boolean)(using storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], environmentStorage: EnvironmentStorage[S], services: BatchEnvironment.Services, priority: AccessControl.Priority) =
    services.replicaCatalog.clean(environmentStorage.id(s), StorageService.rmFile(s, _))
    cleanReplicaDirectory(s, storageSpace.replicaDirectory, environmentStorage.id(s), background)
    cleanTmpDirectory(s, storageSpace.tmpDirectory, background)

  def extractTimeFromName(name: String) =
    val time = name.takeWhile(_.isDigit)
    if (time.isEmpty) None
    else Try(time.toLong).toOption

  def ignoreErrors[T](f: => T): Unit = Try(f)

  def cleanTmpDirectory[S](s: S, tmpDirectory: String, background: Boolean)(using storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], services: BatchEnvironment.Services, priority: AccessControl.Priority) =
    val entries = hierarchicalStorageInterface.list(s, tmpDirectory)
    val removalDate = System.currentTimeMillis - services.preference(TmpDirRemoval).toMillis

    def remove(name: String) = extractTimeFromName(name).map(_ < removalDate).getOrElse(true)

    for
      entry <- entries
      if remove(entry.name)
    do
      val path = StorageService.child(s, tmpDirectory, entry.name)
      if (entry.`type` == FileType.Directory) ignoreErrors(StorageService.rmDirectory(s, path, background))
      else ignoreErrors(StorageService.rmFile(s, path, background))

  def cleanReplicaDirectory[S](s: S, persistentPath: String, storageId: String, background: Boolean)(using services: BatchEnvironment.Services, storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], priority: AccessControl.Priority) =
    def graceIsOver(name: String) =
      extractTimeFromName(name).map {
        _ + services.preference(ReplicaCatalog.ReplicaGraceTime).toMillis < System.currentTimeMillis
      }.getOrElse(true)

    val entries = hierarchicalStorageInterface.list(s, persistentPath)
    val inReplica = services.replicaCatalog.forPaths(entries.map { e => StorageService.child(s, persistentPath, e.name) }, Seq(storageId)).map(_.path).toSet

    for
      e ← entries
      if graceIsOver(e.name)
    do
      val path = StorageService.child(s, persistentPath, e.name)
      if (!inReplica.contains(path))
      then
        if e.`type` == FileType.Directory
        then ignoreErrors(StorageService.rmDirectory(s, path, background))
        else ignoreErrors(StorageService.rmFile(s, path, background))

  def createBasePath[S](s: S, root: String, isConnectionError: Throwable => Boolean)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], preference: Preference, priority: AccessControl.Priority) =
    def baseDirName = "openmole-" + preference(Preference.uniqueID) + '/'

    def mkRootDir: String = synchronized:
      val paths = Iterator.iterate[Option[String]](Some(root))(p => p.flatMap(hierarchicalStorageInterface.parent(s, _))).takeWhile(_.isDefined).toSeq.reverse.flatten

      paths.tail.foldLeft(paths.head):
        (path, file) =>
          val childPath = StorageService.child(s, path, hierarchicalStorageInterface.name(s, file))
          try
            hierarchicalStorageInterface.makeDir(s, childPath)
          catch
            case e: Throwable if isConnectionError(e) => throw e
            case e: Throwable                         => Log.logger.log(Log.FINE, "Error creating base directory " + root, e)

          childPath


    val rootPath = mkRootDir
    val basePath = StorageService.child(s, rootPath, baseDirName)
    util.Try(hierarchicalStorageInterface.makeDir(s, basePath)) match 
      case util.Success(_) => basePath
      case util.Failure(e) =>
        if (isConnectionError(e)) throw e
        else if (storageInterface.exists(s, basePath)) basePath
        else throw e

  def createJobDirectory[S](s: S, storageSpace: StorageSpace)(using hierarchicalStorageInterface: HierarchicalStorageInterface[S], preference: Preference, priority: AccessControl.Priority) = 
    val intervalDirectory = hierarchicalStorageInterface.child(s, storageSpace.tmpDirectory, StorageSpace.lastBegining(preference(TmpDirCreation)))
    ignoreErrors(hierarchicalStorageInterface.makeDir(s, intervalDirectory))
    val communicationPath = hierarchicalStorageInterface.child(s, intervalDirectory, StorageSpace.timedUniqName)
    hierarchicalStorageInterface.makeDir(s, communicationPath)
    communicationPath


case class StorageSpace(baseDirectory: String, replicaDirectory: String, tmpDirectory: String)
