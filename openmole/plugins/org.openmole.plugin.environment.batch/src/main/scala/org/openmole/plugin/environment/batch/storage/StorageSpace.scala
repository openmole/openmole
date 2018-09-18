package org.openmole.plugin.environment.batch.storage

import java.nio.file.spi.FileTypeDetector

import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.replication.ReplicaCatalog
import java.util.regex.Pattern

import scala.util._
import squants.time.TimeConversions._
import gridscale._
import org.openmole.plugin.environment.batch.environment.{ AccessControl, BatchEnvironment }
import org.openmole.plugin.environment.batch.refresh.{ JobManager, RetryAction }
import org.openmole.tool.cache.Lazy
import org.openmole.tool.logger.JavaLogger

object StorageSpace {
  def timedUniqName = org.openmole.tool.file.uniqName(System.currentTimeMillis.toString, ".rep", separator = "_")
}

object HierarchicalStorageSpace extends JavaLogger {
  val TmpDirRemoval = ConfigurationLocation("StorageService", "TmpDirRemoval", Some(30 days))

  def create[S](s: S, root: String, storageId: String, isConnectionError: Throwable ⇒ Boolean)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], preference: Preference) = {
    val persistent = "persistent/"
    val tmp = "tmp/"

    val baseDirectory = createBasePath(s, root, isConnectionError)

    val replicaDirectory = {
      val dir = hierarchicalStorageInterface.child(s, baseDirectory, persistent)
      if (!storageInterface.exists(s, dir)) hierarchicalStorageInterface.makeDir(s, dir)
      dir
    }

    val tmpDirectory = {
      val dir = hierarchicalStorageInterface.child(s, baseDirectory, tmp)
      if (!storageInterface.exists(s, dir)) hierarchicalStorageInterface.makeDir(s, dir)
      dir
    }

    StorageSpace(baseDirectory, replicaDirectory, tmpDirectory)
  }

  def clean[S](s: S, storageSpace: StorageSpace)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], environmentStorage: EnvironmentStorage[S], services: BatchEnvironment.Services) = {
    services.replicaCatalog.clean(environmentStorage.id(s), StorageService.rmFile(s, _))
    cleanReplicaDirectory(s, storageSpace.replicaDirectory, environmentStorage.id(s))
    cleanTmpDirectory(s, storageSpace.tmpDirectory)
  }

  lazy val replicationPattern = Pattern.compile("(\\p{XDigit}*)_.*")
  def extractTimeFromName(name: String) = {
    val matcher = replicationPattern.matcher(name)
    if (!matcher.matches) None
    else Try(matcher.group(1).toLong).toOption
  }

  def cleanTmpDirectory[S](s: S, tmpDirectory: String)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], services: BatchEnvironment.Services) = {
    val entries = hierarchicalStorageInterface.list(s, tmpDirectory)
    val removalDate = System.currentTimeMillis - services.preference(TmpDirRemoval).toMillis

    def remove(name: String) = extractTimeFromName(name).map(_ < removalDate).getOrElse(true)

    for {
      entry ← entries
      if remove(entry.name)
    } {
      val path = StorageService.child(s, tmpDirectory, entry.name)
      if (entry.`type` == FileType.Directory) StorageService.rmDirectory(s, path)
      else StorageService.rmFile(s, path)
    }
  }

  def cleanReplicaDirectory[S](s: S, persistentPath: String, storageId: String)(implicit services: BatchEnvironment.Services, storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S]) = {
    def graceIsOver(name: String) =
      extractTimeFromName(name).map {
        _ + services.preference(ReplicaCatalog.ReplicaGraceTime).toMillis < System.currentTimeMillis
      }.getOrElse(true)

    val entries = hierarchicalStorageInterface.list(s, persistentPath)
    val inReplica = services.replicaCatalog.forPaths(entries.map { e ⇒ StorageService.child(s, persistentPath, e.name) }, Seq(storageId)).map(_.path).toSet

    for {
      e ← entries
      if graceIsOver(e.name)
    } {
      val path = StorageService.child(s, persistentPath, e.name)
      if (!inReplica.contains(path)) if (e.`type` == FileType.Directory) StorageService.rmDirectory(s, path) else StorageService.rmFile(s, path)
    }
  }

  def createBasePath[S](s: S, root: String, isConnectionError: Throwable ⇒ Boolean)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], preference: Preference) = {
    def baseDirName = "openmole-" + preference(Preference.uniqueID) + '/'

    def mkRootDir: String = synchronized {
      val paths = Iterator.iterate[Option[String]](Some(root))(p ⇒ p.flatMap(hierarchicalStorageInterface.parent(s, _))).takeWhile(_.isDefined).toSeq.reverse.flatten

      paths.tail.foldLeft(paths.head.toString) {
        (path, file) ⇒
          val childPath = StorageService.child(s, path, hierarchicalStorageInterface.name(s, file))
          try hierarchicalStorageInterface.makeDir(s, childPath)
          catch {
            case e: Throwable if isConnectionError(e) ⇒ throw e
            case e: Throwable                         ⇒ Log.logger.log(Log.FINE, "Error creating base directory " + root, e)
          }
          childPath
      }
    }

    val rootPath = mkRootDir
    val basePath = StorageService.child(s, rootPath, baseDirName)
    util.Try(hierarchicalStorageInterface.makeDir(s, basePath)) match {
      case util.Success(_) ⇒ basePath
      case util.Failure(e) ⇒
        if (storageInterface.exists(s, basePath)) basePath else throw e
    }
  }

  def createJobDirectory[S](s: S, storageSpace: StorageSpace)(implicit hierarchicalStorageInterface: HierarchicalStorageInterface[S]) = {
    val communicationPath = hierarchicalStorageInterface.child(s, storageSpace.tmpDirectory, StorageSpace.timedUniqName)
    hierarchicalStorageInterface.makeDir(s, communicationPath)
    communicationPath
  }
}

case class StorageSpace(baseDirectory: String, replicaDirectory: String, tmpDirectory: String)
