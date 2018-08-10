package org.openmole.plugin.environment.batch.storage

import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.replication.ReplicaCatalog
import java.util.regex.Pattern

import scala.util._
import squants.time.TimeConversions._
import gridscale._
import org.openmole.tool.logger.JavaLogger

object StorageSpace extends JavaLogger {
  val TmpDirRemoval = ConfigurationLocation("StorageService", "TmpDirRemoval", Some(30 days))

  def hierarchicalStorageSpace[S](s: S, root: String, storageId: String, isConnectionError: Throwable ⇒ Boolean)(implicit storageInterface: StorageInterface[S], preference: Preference, replicaCatalog: ReplicaCatalog) = {
    val persistent = "persistent/"
    val tmp = "tmp/"

    val baseDirectory = createBasePath(s, root, isConnectionError)

    val replicaDirectory = {
      val dir = storageInterface.child(s, baseDirectory, persistent)
      if (!storageInterface.exists(s, dir)) storageInterface.makeDir(s, dir)
      cleanReplicaDirectory(s, dir, storageId)
      dir
    }

    val tmpDirectory = {
      val dir = storageInterface.child(s, baseDirectory, tmp)
      if (!storageInterface.exists(s, dir)) storageInterface.makeDir(s, dir)
      cleanTmpDirectory(s, dir)
      dir
    }

    StorageSpace(baseDirectory, replicaDirectory, tmpDirectory)
  }

  def timedUniqName = org.openmole.tool.file.uniqName(System.currentTimeMillis.toString, ".rep", separator = "_")

  lazy val replicationPattern = Pattern.compile("(\\p{XDigit}*)_.*")
  def extractTimeFromName(name: String) = {
    val matcher = replicationPattern.matcher(name)
    if (!matcher.matches) None
    else Try(matcher.group(1).toLong).toOption
  }

  def cleanTmpDirectory[S](s: S, tmpDirectory: String)(implicit storageInterface: StorageInterface[S], preference: Preference) = {
    val entries = storageInterface.list(s, tmpDirectory)
    val removalDate = System.currentTimeMillis - preference(TmpDirRemoval).toMillis

    def remove(name: String) = extractTimeFromName(name).map(_ < removalDate).getOrElse(true)

    for {
      entry ← entries
      if remove(entry.name)
    } {
      val path = storageInterface.child(s, tmpDirectory, entry.name)
      if (entry.`type` == FileType.Directory) storageInterface.rmDir(s, path)
      else storageInterface.rmFile(s, path)
    }
  }

  def cleanReplicaDirectory[S](s: S, persistentPath: String, storageId: String)(implicit replicaCatalog: ReplicaCatalog, preference: Preference, storageInterface: StorageInterface[S]) = {
    def graceIsOver(name: String) =
      extractTimeFromName(name).map {
        _ + preference(ReplicaCatalog.ReplicaGraceTime).toMillis < System.currentTimeMillis
      }.getOrElse(true)

    val names = storageInterface.list(s, persistentPath).map(_.name)
    val inReplica = replicaCatalog.forPaths(names.map { storageInterface.child(s, persistentPath, _) }, Seq(storageId)).map(_.path).toSet

    for {
      name ← names
      if graceIsOver(name)
    } {
      val path = storageInterface.child(s, persistentPath, name)
      if (!inReplica.contains(path)) storageInterface.rmFile(s, path)
    }
  }

  def createBasePath[S](s: S, root: String, isConnectionError: Throwable ⇒ Boolean)(implicit storageInterface: StorageInterface[S], preference: Preference) = {
    def baseDirName = "openmole-" + preference(Preference.uniqueID) + '/'

    def mkRootDir: String = synchronized {
      val paths = Iterator.iterate[Option[String]](Some(root))(p ⇒ p.flatMap(storageInterface.parent(s, _))).takeWhile(_.isDefined).toSeq.reverse.flatten

      paths.tail.foldLeft(paths.head.toString) {
        (path, file) ⇒
          val childPath = storageInterface.child(s, path, storageInterface.name(s, file))
          try storageInterface.makeDir(s, childPath)
          catch {
            case e: Throwable if isConnectionError(e) ⇒ throw e
            case e: Throwable                         ⇒ Log.logger.log(Log.FINE, "Error creating base directory " + root, e)
          }
          childPath
      }
    }

    val rootPath = mkRootDir
    val basePath = storageInterface.child(s, rootPath, baseDirName)
    util.Try(storageInterface.makeDir(s, basePath)) match {
      case util.Success(_) ⇒ basePath
      case util.Failure(e) ⇒
        if (storageInterface.exists(s, basePath)) basePath else throw e
    }
  }

}
case class StorageSpace(baseDirectory: String, replicaDirectory: String, tmpDirectory: String)
