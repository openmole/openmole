/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.replication

import java.io.File
import java.util.concurrent.TimeUnit

import com.google.common.cache._
import org.openmole.core.db._
import org.openmole.core.preference._
import org.openmole.core.workspace.Workspace
import org.openmole.tool.lock.LockRepository
import org.openmole.tool.logger.JavaLogger
import squants.time.TimeConversions._

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.util.{ Success, Try }

object ReplicaCatalog extends JavaLogger {
  val NoAccessCleanTime = PreferenceLocation("ReplicaCatalog", "NoAccessCleanTime", Some(30 days))
  val ReplicaCacheTime = PreferenceLocation("ReplicaCatalog", "ReplicaCacheTime", Some(30 minutes))
  val ReplicaCacheSize = PreferenceLocation("ReplicaCatalog", "ReplicaCacheSize", Some(1000))
  val ReplicaGraceTime = PreferenceLocation("ReplicaCatalog", "ReplicaGraceTime", Some(1 days))
  val LockTimeout = PreferenceLocation("ReplicaCatalog", "LockTimeout", Some(1 minutes))
  val CheckFileExistsInterval = PreferenceLocation("ReplicaCatalog", "CheckFileExistsInterval", Some(30 minutes))

  def apply(workspace: Workspace)(implicit preference: Preference): ReplicaCatalog =
    val dbDirectory = org.openmole.core.db.dbDirectory(workspace.persistentDir)
    new ReplicaCatalog(org.openmole.core.db.databaseServer(dbDirectory, preference(LockTimeout)), preference)

  def apply(database: Database)(implicit preference: Preference): ReplicaCatalog =
    new ReplicaCatalog(database, preference)

}

/**
 * Manage [[Replica]]s in the database
 * @param database
 * @param preference
 */
class ReplicaCatalog(database: Database, preference: Preference) {

  import ReplicaCatalog.Log._
  import ReplicaCatalog._

  def close() = database.close()

   lazy val localLock = new LockRepository[ReplicaCacheKey]

  type ReplicaCacheKey = (String, String, String)

  val replicaCache = CacheBuilder.newBuilder.asInstanceOf[CacheBuilder[ReplicaCacheKey, Replica]].
    maximumSize(preference(ReplicaCacheSize).toLong).
    expireAfterAccess(preference(ReplicaCacheTime).millis, TimeUnit.MILLISECONDS).
    build[ReplicaCacheKey, Replica]

  def clean(storageId: String, removeOnStorage: String => Unit) = {
    val time = System.currentTimeMillis

    // Note: Destination file will be cleaned while cleaning the replicaDirectory
    for {
      replica ← database.selectOnStorage(storageId)
      if !new File(replica.source).exists || time - replica.lastCheckExists > preference(ReplicaCatalog.NoAccessCleanTime).millis
    } remove(replica.id)

  }

  def uploadAndGet[S](
    upload:    => String,
    exists:    String => Boolean,
    remove:    String => Unit,
    srcPath:   File,
    hash:      String,
    storageId: String
  ): Replica = {
    val cacheKey = (srcPath.getCanonicalPath, hash, storageId)
    // Avoid same transfer in multiple threads
    localLock.locked(cacheKey) {
      uploadAndGetLocked(
        upload = upload,
        exists = exists,
        remove = remove,
        srcPath = srcPath,
        hash = hash,
        storageId = storageId,
        cacheKey = cacheKey)
    }
  }

  @tailrec private def uploadAndGetLocked(
    upload:    => String,
    exists:    String => Boolean,
    remove:    String => Unit,
    srcPath:   File,
    hash:      String,
    storageId: String,
    cacheKey:  ReplicaCacheKey
  ): Replica = {
    val replica =
      Option(replicaCache.getIfPresent(cacheKey)) match {
        case Some(r) => r
        case None =>
          //If replica is already present on the storage with another hash
          def cleanOldReplicas = {
            val sameSource = database.deleteSameSourceWithDifferentHash(srcPath.getCanonicalPath, storageId, hash)
            sameSource.foreach { replica =>
              logger.fine(s"Remove obsolete $replica")
              remove(replica.path)
            }
          }

          def uploadAndInsertIfNotInCatalog: Replica = {
            val replicas = database.selectSameSource(srcPath.getCanonicalPath, storageId, hash)
              
            import scala.concurrent.ExecutionContext.Implicits.global

            replicas.lastOption match {
              case Some(r) => r
              case None =>
                val newFile = upload

                sealed trait InsertionResult
                case class AlreadyInDb(remoteFile: String, replica: Replica) extends InsertionResult
                case class Inserted(replica: Replica) extends InsertionResult

                val inserted = database.insert(srcPath.getCanonicalPath, storageId, newFile, hash, System.currentTimeMillis)

                inserted match {
                  case Transactor.AlreadyInDb(replica) =>
                    remove(newFile)
                    replica
                  case Transactor.Inserted(replica) => replica
                }
            }
          }

          cleanOldReplicas
          val replica = uploadAndInsertIfNotInCatalog
          replicaCache.put(cacheKey, replica)
          replica
      }

    def itsTimeToCheck(r: Replica) = r.lastCheckExists + preference(CheckFileExistsInterval).toMillis < System.currentTimeMillis
    def stillExists(r: Replica) =
      Try(exists(r.path)) match {
        case Success(e) => e
        case _          => false
      }

    if (itsTimeToCheck(replica)) {
      if (stillExists(replica)) {
        val newReplica = replica.copy(lastCheckExists = System.currentTimeMillis())
        database.updateLastCheckExists(replica.id, newReplica.lastCheckExists)
        replicaCache.put(cacheKey, newReplica)
        newReplica
      }
      else {
        replicaCache.invalidate(cacheKey)
        database.delete(replica.id)
        uploadAndGetLocked(
          upload = upload,
          exists = exists,
          remove = remove,
          srcPath = srcPath,
          hash = hash,
          storageId = storageId,
          cacheKey = cacheKey)
      }
    }
    else replica
  }

  def forPaths(paths: Seq[String], storageId: Seq[String]): Seq[Replica] = database.selectPathsStorages(paths, storageId)
  def forHashes(hashes: Seq[String], storageId: Seq[String]): Seq[Replica] = database.selectHashesStorages(hashes, storageId)

  def deleteReplicas(storageId: String): Unit = {
    val replica = database.deleteOnStorage(storageId)
    replica.foreach { r => replicaCache.invalidate(cacheKey(r)) }
  }

  private def cacheKey(r: Replica) = (r.source, r.hash, r.storage)

  def remove(id: Long) = {
    val replica = database.delete(id)

    val (source, storage, path) = if (replica.nonEmpty) (replica.get.source, replica.get.storage, replica.get.path) else ("None", "None", "None")
    logger.fine(s"Remove replica with id $id, from source $source, storage $storage, path $path")

    replica.foreach { r => replicaCache.invalidate(cacheKey(r)) }
  }

}
