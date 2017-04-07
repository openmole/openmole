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
import java.util.regex.Pattern

import com.google.common.cache._
import org.openmole.core.db
import org.openmole.core.preference._
import org.openmole.core.db.{ Replica, replicas }
import org.openmole.core.workspace.Workspace
import org.openmole.tool.cache.TimeCache
import org.openmole.tool.lock.LockRepository
import org.openmole.tool.logger.Logger
import slick.driver.H2Driver.api._
import squants.time.TimeConversions._

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{ Success, Try }

object ReplicaCatalog extends Logger {
  val NoAccessCleanTime = ConfigurationLocation("ReplicaCatalog", "NoAccessCleanTime", Some(30 days))
  val InCatalogCacheTime = ConfigurationLocation("ReplicaCatalog", "InCatalogCacheTime", Some(2 minutes))
  val ReplicaCacheTime = ConfigurationLocation("ReplicaCatalog", "ReplicaCacheTime", Some(30 minutes))
  val ReplicaGraceTime = ConfigurationLocation("ReplicaCatalog", "ReplicaGraceTime", Some(1 days))
  val LockTimeout = ConfigurationLocation("ReplicaCatalog", "LockTimeout", Some(1 minutes))
  val CheckFileExistsInterval = ConfigurationLocation("ReplicaCatalog", "CheckFileExistsInterval", Some(1 hours))

  def apply()(implicit preference: Preference, workspace: Workspace): ReplicaCatalog = {
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

  lazy val inCatalogCache = new TimeCache[Map[String, Set[String]]]

  def query[T](f: DBIOAction[T, slick.dbio.NoStream, scala.Nothing]) = Await.result(database.run(f), concurrent.duration.Duration.Inf)

  lazy val localLock = new LockRepository[ReplicaCacheKey]

  type ReplicaCacheKey = (String, String, String)
  val replicaCache = CacheBuilder.newBuilder.asInstanceOf[CacheBuilder[ReplicaCacheKey, Replica]].
    expireAfterWrite(preference(ReplicaCacheTime).millis, TimeUnit.MILLISECONDS).build[ReplicaCacheKey, Replica]

  def inCatalog = inCatalogCache(inCatalogQuery, preference(InCatalogCacheTime).millis)

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
    localLock.withLock(cacheKey) {
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

          @tailrec def uploadAndInsertIfNotInCatalog: Replica = {
            //Remove deleted replicas
            def stillExists(r: Replica) =
              if (r.lastCheckExists + preference(CheckFileExistsInterval).toMillis < System.currentTimeMillis) {
                Try(replicationStorage.exists(storage, r.path)) match {
                  case Success(e) ⇒ e
                  case _          ⇒ false
                }
              }
              else true

            def getReplica =
              replicas.filter { r ⇒ r.source === srcPath.getCanonicalPath && r.storage === replicationStorage.id(storage) && r.hash === hash }

            def assertReplica(r: Replica) = {
              assert(r.path != null)
              assert(r.storage != null)
              assert(r.hash != null)
              r
            }

            import scala.concurrent.ExecutionContext.Implicits.global

            val replica =
              query(getReplica.result).lastOption.getOrElse {
                val newFile = upload

                val action =
                  getReplica.result.map(_.lastOption).map {
                    case Some(r) ⇒ (r, Some(newFile))
                    case None ⇒
                      val newReplica = Replica(
                        source = srcPath.getCanonicalPath,
                        storage = replicationStorage.id(storage),
                        path = newFile,
                        hash = hash,
                        lastCheckExists = System.currentTimeMillis
                      )
                      replicas += newReplica
                      (newReplica, Option.empty[String])
                  }.transactionally

                val (replica, delete) = query { action }

                delete.foreach(replicationStorage.exists(storage, _))
                replica
              }

            if (stillExists(replica)) replica
            else {
              remove(replica.id)
              uploadAndInsertIfNotInCatalog
            }
          }

          cleanOldReplicas
          val replica = uploadAndInsertIfNotInCatalog
          replicaCache.put(cacheKey, replica)
          replica
      }
    }
  }

  def forPaths(paths: Seq[String]) = query { replicas.filter(_.path inSetBind paths).result }
  def deleteReplicas[S](storage: S)(implicit replicationStorage: ReplicationStorage[S]): Unit = deleteReplicas(replicationStorage.id(storage))
  def deleteReplicas(storageId: String): Unit = query { replicas.filter { _.storage === storageId }.delete }

  def remove(id: Long) = query {
    logger.fine(s"Remove replica with id $id")
    replicas.filter { _.id === id }.delete
  }

  def timeOfPersistent(name: String) = {
    val matcher = replicationPattern.matcher(name)
    if (!matcher.matches) None
    else Try(matcher.group(1).toLong).toOption
  }

}
