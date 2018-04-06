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
import java.sql.DriverAction
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import com.google.common.cache._
import org.openmole.core.db
import org.openmole.core.preference._
import org.openmole.core.db.{ DBServerInfo, Replica, replicas }
import org.openmole.core.workspace.Workspace
import org.openmole.tool.lock.LockRepository
import org.openmole.tool.logger.JavaLogger
import slick.jdbc.H2Profile.api._
import squants.time.TimeConversions._

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
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

trait ReplicationStorage[T] {
  def backgroundRmFile(storage: T, path: String): Unit
  def exists(storage: T, path: String): Boolean
  def id(storage: T): String
}

class ReplicaCatalog(database: Database, preference: Preference) {

  import ReplicaCatalog.Log._
  import ReplicaCatalog._

  lazy val replicationPattern = Pattern.compile("(\\p{XDigit}*)_.*")

  def query[T](f: DBIOAction[T, slick.dbio.NoStream, scala.Nothing]): T = Await.result(database.run(f), concurrent.duration.Duration.Inf)

  lazy val localLock = new LockRepository[ReplicaCacheKey]
  type ReplicaCacheKey = (String, String, String)

  val replicaCache = CacheBuilder.newBuilder.asInstanceOf[CacheBuilder[ReplicaCacheKey, Replica]].
    maximumSize(preference(ReplicaCacheSize)).
    expireAfterAccess(preference(ReplicaCacheTime).millis, TimeUnit.MILLISECONDS).
    build[ReplicaCacheKey, Replica]

  private def inCatalogQuery: Map[String, Set[String]] = {
    val all = query(replicas.map { replica ⇒ (replica.storage, replica.hash) }.result)
    all.groupBy(_._1).mapValues { _.map { case (_, hash) ⇒ hash }.toSet }.withDefaultValue(Set.empty)
  }

  def uploadAndGet[S](
    upload:  ⇒ String,
    srcPath: File,
    hash:    String,
    storage: S
  )(implicit replicationStorage: ReplicationStorage[S]): Replica = {
    val cacheKey = (srcPath.getCanonicalPath, hash, replicationStorage.id(storage))
    // Avoid same transfer in multiple threads
    localLock.withLock(cacheKey) { uploadAndGetLocked(upload, srcPath, hash, storage, cacheKey) }
  }

  @tailrec private def uploadAndGetLocked[S](
    upload:   ⇒ String,
    srcPath:  File,
    hash:     String,
    storage:  S,
    cacheKey: ReplicaCacheKey
  )(implicit replicationStorage: ReplicationStorage[S]): Replica = {
    val replica =
      Option(replicaCache.getIfPresent(cacheKey)) match {
        case Some(r) ⇒ r
        case None ⇒
          //If replica is already present on the storage with another hash
          def cleanOldReplicas = {
            val getReplicasForSrcWithOtherHash =
              replicas.filter { r ⇒
                r.source === srcPath.getCanonicalPath && r.hash =!= hash && r.storage === replicationStorage.id(storage)
              }

            val samePath = query(getReplicasForSrcWithOtherHash.result)
            samePath.foreach {
              replica ⇒
                logger.fine(s"Remove obsolete $replica")
                replicationStorage.backgroundRmFile(storage, replica.path)
            }
            query(getReplicasForSrcWithOtherHash.delete)
          }

          def uploadAndInsertIfNotInCatalog: Replica = {
            def getReplica =
              replicas.filter { r ⇒ r.source === srcPath.getCanonicalPath && r.storage === replicationStorage.id(storage) && r.hash === hash }

            import scala.concurrent.ExecutionContext.Implicits.global

            query(getReplica.result).lastOption.getOrElse {
              val newFile = upload

              sealed trait InsertionResult
              case class AlreadyInDb(remoteFile: String, replica: Replica) extends InsertionResult
              case class Inserted(replica: Replica) extends InsertionResult

              val inserted =
                query {
                  getReplica.result.map(_.lastOption).flatMap {
                    case Some(r) ⇒ DBIO.successful(AlreadyInDb(newFile, r))
                    case None ⇒
                      val newReplica = Replica(
                        source = srcPath.getCanonicalPath,
                        storage = replicationStorage.id(storage),
                        path = newFile,
                        hash = hash,
                        lastCheckExists = System.currentTimeMillis
                      )

                      (replicas += newReplica).map(_ ⇒ Inserted(newReplica))
                  }.transactionally
                }

              inserted match {
                case AlreadyInDb(remoteFile, replica) ⇒
                  replicationStorage.backgroundRmFile(storage, remoteFile)
                  replica
                case Inserted(replica) ⇒
                  replica
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
      Try(replicationStorage.exists(storage, r.path)) match {
        case Success(e) ⇒ e
        case _          ⇒ false
      }

    if (itsTimeToCheck(replica)) {
      replicaCache.invalidate(cacheKey)
      if (stillExists(replica)) {
        val newReplica = replica.copy(lastCheckExists = System.currentTimeMillis())
        query(replicas.filter(_.id === replica.id).map(_.lastCheckExists).update(newReplica.lastCheckExists).transactionally)
        newReplica
      }
      else {
        query(replicas.filter(_.id === replica.id).delete)
        uploadAndGetLocked(upload, srcPath, hash, storage, cacheKey)
      }
    }
    else replica
  }

  def forPaths(paths: Seq[String], storageId: Seq[String]) = query { replicas.filter(r ⇒ (r.path inSetBind paths) && (r.storage inSetBind storageId)).result }
  def forHashes(hashes: Seq[String], storageId: Seq[String]) = query { replicas.filter(r ⇒ (r.hash inSetBind hashes) && (r.storage inSetBind storageId)).result }

  def deleteReplicas[S](storageId: S)(implicit replicationStorage: ReplicationStorage[S]): Unit = deleteReplicas(replicationStorage.id(storageId))
  def deleteReplicas(storageId: String): Unit = query { replicas.filter { _.storage === storageId }.delete }

  private def cacheKey(r: Replica) = (r.source, r.hash, r.storage)

  def remove(id: Long) = {
    logger.fine(s"Remove replica with id $id")

    val replica = query { replicas.filter(_.id === id).result }.headOption
    query { replicas.filter { _.id === id }.delete }

    replica.foreach { r ⇒ replicaCache.invalidate(cacheKey(r)) }
  }

  def timeOfPersistent(name: String) = {
    val matcher = replicationPattern.matcher(name)
    if (!matcher.matches) None
    else Try(matcher.group(1).toLong).toOption
  }

}
