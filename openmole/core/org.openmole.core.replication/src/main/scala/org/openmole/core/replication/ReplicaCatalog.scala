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
import java.util.concurrent.locks.ReentrantLock

import com.google.common.cache._
import org.openmole.core.db.{ Replica, replicas }
import org.openmole.core.preference._
import org.openmole.core.workspace.Workspace
import org.openmole.tool.lock.LockRepository
import org.openmole.tool.logger.JavaLogger
import slick.jdbc.H2Profile.api._
import squants.time.TimeConversions._

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.util.{ Success, Try }

object ReplicaCatalog extends JavaLogger {
  val NoAccessCleanTime = ConfigurationLocation("ReplicaCatalog", "NoAccessCleanTime", Some(30 days))
  val ReplicaCacheTime = ConfigurationLocation("ReplicaCatalog", "ReplicaCacheTime", Some(30 minutes))
  val ReplicaCacheSize = ConfigurationLocation("ReplicaCatalog", "ReplicaCacheSize", Some(1000))
  val ReplicaGraceTime = ConfigurationLocation("ReplicaCatalog", "ReplicaGraceTime", Some(1 days))
  val LockTimeout = ConfigurationLocation("ReplicaCatalog", "LockTimeout", Some(1 minutes))
  val CheckFileExistsInterval = ConfigurationLocation("ReplicaCatalog", "CheckFileExistsInterval", Some(30 minutes))

  def apply(workspace: Workspace)(implicit preference: Preference): ReplicaCatalog = {
    val dbDirectory = org.openmole.core.db.dbDirectory(workspace.location)
    new ReplicaCatalog(org.openmole.core.db.databaseServer(dbDirectory, preference(LockTimeout)), preference)
  }

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

  def query[T](f: DBIOAction[T, slick.dbio.NoStream, scala.Nothing]): T = Await.result(database.run(f), concurrent.duration.Duration.Inf)

  lazy val localLock = new LockRepository[ReplicaCacheKey]

  type ReplicaCacheKey = (String, String, String)

  val replicaCache = CacheBuilder.newBuilder.asInstanceOf[CacheBuilder[ReplicaCacheKey, Replica]].
    maximumSize(preference(ReplicaCacheSize)).
    expireAfterAccess(preference(ReplicaCacheTime).millis, TimeUnit.MILLISECONDS).
    build[ReplicaCacheKey, Replica]

  def clean(storageId: String, removeOnStorage: String ⇒ Unit) = {
    val time = System.currentTimeMillis

    // Note: Destination file will be cleaned while cleaning the replicaDirectory
    for {
      replica ← query { replicas.filter { _.storage === storageId }.result }
      if !new File(replica.source).exists || time - replica.lastCheckExists > preference(ReplicaCatalog.NoAccessCleanTime).millis
    } remove(replica.id)

  }

  def uploadAndGet[S](
    upload:    ⇒ String,
    exists:    String ⇒ Boolean,
    remove:    String ⇒ Unit,
    srcPath:   File,
    hash:      String,
    storageId: String
  ): Replica = {
    val cacheKey = (srcPath.getCanonicalPath, hash, storageId)
    // Avoid same transfer in multiple threads
    localLock.withLock(cacheKey) {
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
    upload:    ⇒ String,
    exists:    String ⇒ Boolean,
    remove:    String ⇒ Unit,
    srcPath:   File,
    hash:      String,
    storageId: String,
    cacheKey:  ReplicaCacheKey
  ): Replica = {
    val replica =
      Option(replicaCache.getIfPresent(cacheKey)) match {
        case Some(r) ⇒ r
        case None ⇒
          //If replica is already present on the storage with another hash
          def cleanOldReplicas = {
            val getReplicasForSrcWithOtherHash =
              replicas.filter { r ⇒
                r.source === srcPath.getCanonicalPath && r.hash =!= hash && r.storage === storageId
              }

            val samePath = query(getReplicasForSrcWithOtherHash.result)
            samePath.foreach {
              replica ⇒
                logger.fine(s"Remove obsolete $replica")
                remove(replica.path)
            }

            query(getReplicasForSrcWithOtherHash.delete)
          }

          def uploadAndInsertIfNotInCatalog: Replica = {
            def getReplica =
              replicas.filter { r ⇒ r.source === srcPath.getCanonicalPath && r.storage === storageId && r.hash === hash }

            import scala.concurrent.ExecutionContext.Implicits.global

            query(getReplica.result).lastOption match {
              case Some(r) ⇒ r
              case None ⇒
                val newFile = upload

                sealed trait InsertionResult
                case class AlreadyInDb(remoteFile: String, replica: Replica) extends InsertionResult
                case class Inserted(replica: Replica) extends InsertionResult

                val inserted =
                  query {
                    val insert = getReplica.result.map(_.lastOption).flatMap {
                      case Some(r) ⇒ DBIO.successful(AlreadyInDb(newFile, r))
                      case None ⇒
                        val newReplica = Replica(
                          source = srcPath.getCanonicalPath,
                          storage = storageId,
                          path = newFile,
                          hash = hash,
                          lastCheckExists = System.currentTimeMillis
                        )

                        (replicas += newReplica).map(_ ⇒ Inserted(newReplica))
                    }

                    insert.transactionally
                  }

                inserted match {
                  case AlreadyInDb(remoteFile, replica) ⇒
                    remove(remoteFile)
                    replica
                  case Inserted(replica) ⇒ replica
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
        case Success(e) ⇒ e
        case _          ⇒ false
      }

    if (itsTimeToCheck(replica)) {
      if (stillExists(replica)) {
        val newReplica = replica.copy(lastCheckExists = System.currentTimeMillis())
        query(replicas.filter(_.id === replica.id).map(_.lastCheckExists).update(newReplica.lastCheckExists).transactionally)
        replicaCache.put(cacheKey, newReplica)
        newReplica
      }
      else {
        replicaCache.invalidate(cacheKey)
        query(replicas.filter(_.id === replica.id).delete)
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

  def forPaths(paths: Seq[String], storageId: Seq[String]) = query { replicas.filter(r ⇒ (r.path inSetBind paths) && (r.storage inSetBind storageId)).result }
  def forHashes(hashes: Seq[String], storageId: Seq[String]) = query { replicas.filter(r ⇒ (r.hash inSetBind hashes) && (r.storage inSetBind storageId)).result }

  def deleteReplicas(storageId: String): Unit = {
    def q = replicas.filter { _.storage === storageId }
    val replica = query { q.result }.headOption
    query { q.delete }
    replica.foreach { r ⇒ replicaCache.invalidate(cacheKey(r)) }
  }

  private def cacheKey(r: Replica) = (r.source, r.hash, r.storage)

  def remove(id: Long) = {

    def q = replicas.filter(_.id === id)
    val replica = query { q.result }.headOption

    val (source, storage, path) = if (replica.nonEmpty) (replica.get.source, replica.get.storage, replica.get.path) else ("None", "None", "None")
    logger.fine(s"Remove replica with id $id, from source $source, storage $storage, path $path")

    query { q.delete }

    replica.foreach { r ⇒ replicaCache.invalidate(cacheKey(r)) }
  }

}
