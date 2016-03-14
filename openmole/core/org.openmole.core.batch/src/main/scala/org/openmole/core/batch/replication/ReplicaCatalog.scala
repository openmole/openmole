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

package org.openmole.core.batch.replication

import com.google.common.cache.CacheBuilder
import java.io.File
import org.h2.jdbc.JdbcSQLException
import org.openmole.core.replication.{ replicas, Replica, DBServerInfo }
import org.openmole.core.tools.service.TimeCache
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import java.util.regex.Pattern
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.environment.BatchEnvironment._
import java.util.concurrent.TimeUnit

import org.openmole.tool.lock.LockRepository
import org.openmole.tool.logger.Logger
import slick.jdbc.SQLActionBuilder
import slick.profile.SqlAction

import scala.annotation.tailrec
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import slick.driver.H2Driver.api._
import scala.util.{ Success, Failure, Try }

object ReplicaCatalog extends Logger {

  import Log._

  val NoAccessCleanTime = ConfigurationLocation("ReplicaCatalog", "NoAccessCleanTime", Some(30 days))
  val InCatalogCacheTime = ConfigurationLocation("ReplicaCatalog", "InCatalogCacheTime", Some(2 minutes))
  val ReplicaCacheTime = ConfigurationLocation("ReplicaCatalog", "ReplicaCacheTime", Some(30 minutes))
  val ReplicaGraceTime = ConfigurationLocation("ReplicaCatalog", "ReplicaGraceTime", Some(1 day))
  val LockTimeout = ConfigurationLocation("ReplicaCatalog", "LockTimeout", Some(1 minute))

  Workspace setDefault NoAccessCleanTime
  Workspace setDefault InCatalogCacheTime
  Workspace setDefault ReplicaCacheTime
  Workspace setDefault ReplicaGraceTime
  Workspace setDefault LockTimeout

  lazy val replicationPattern = Pattern.compile("(\\p{XDigit}*)_.*")

  lazy val inCatalogCache = new TimeCache[Map[String, Set[String]]]
  lazy val database = {
    val dbInfoFile = DBServerInfo.dbInfoFile
    val info = DBServerInfo.load(dbInfoFile)
    val db = Database.forDriver(
      driver = new org.h2.Driver,
      url = s"jdbc:h2:tcp://localhost:${info.port}/${DBServerInfo.dbDirectory}/${DBServerInfo.urlDBPath}",
      user = info.user,
      password = info.password
    )

    Await.result(db.run { sqlu"""SET DEFAULT_LOCK_TIMEOUT ${Workspace.preference(LockTimeout).toMillis}""" }, Duration.Inf)
    db
  }

  def query[T](f: DBIOAction[T, slick.dbio.NoStream, scala.Nothing]) = Await.result(database.run(f), Duration.Inf)

  lazy val localLock = new LockRepository[ReplicaCacheKey]

  type ReplicaCacheKey = (String, String, String)
  val replicaCache = CacheBuilder.newBuilder.asInstanceOf[CacheBuilder[ReplicaCacheKey, Replica]].
    expireAfterWrite(Workspace.preference(ReplicaCacheTime).toSeconds, TimeUnit.SECONDS).build[ReplicaCacheKey, Replica]

  def inCatalog = inCatalogCache(inCatalogQuery, Workspace.preference(InCatalogCacheTime).toMillis)

  private def inCatalogQuery: Map[String, Set[String]] = {
    val all = query(replicas.map { replica ⇒ (replica.storage, replica.hash) }.result)
    all.groupBy(_._1).mapValues { _.map { case (_, hash) ⇒ hash }.toSet }.withDefaultValue(Set.empty)
  }

  def uploadAndGet(
    upload: ⇒ String,
    srcPath: File,
    hash: String,
    storage: StorageService)(implicit token: AccessToken): Replica = {

    val cacheKey = (srcPath.getCanonicalPath, hash, storage.id)

    // Avoid same transfer in multiple threads
    localLock.withLock(cacheKey) {
      Option(replicaCache.getIfPresent(cacheKey)) match {
        case Some(r) ⇒ r
        case None ⇒

          //If replica is already present on the storage with another hash
          def cleanOldReplicas = {
            val getReplicasForSrcWithOtherHash =
              replicas.filter { r ⇒
                r.source === srcPath.getCanonicalPath && r.hash =!= hash && r.storage === storage.id
              }

            val samePath = query(getReplicasForSrcWithOtherHash.result)
            samePath.foreach {
              replica ⇒
                logger.fine(s"Remove obsolete $replica")
                storage.backgroundRmFile(replica.path)
            }
            query(getReplicasForSrcWithOtherHash.delete)
          }

          @tailrec def uploadAndInsertIfNotInCatalog: Replica = {
            //Remove deleted replicas
            def stillExists(r: Replica) =
              if (r.lastCheckExists + Workspace.preference(BatchEnvironment.CheckFileExistsInterval).toMillis < System.currentTimeMillis) {
                Try(storage.exists(r.path)) match {
                  case Success(e) ⇒ e
                  case _          ⇒ false
                }
              }
              else true

            def getReplica =
              replicas.filter { r ⇒ r.source === srcPath.getCanonicalPath && r.storage === storage.id && r.hash === hash }

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
                        storage = storage.id,
                        path = newFile,
                        hash = hash,
                        lastCheckExists = System.currentTimeMillis
                      )
                      replicas += newReplica
                      (newReplica, Option.empty[String])
                  }.transactionally

                val (replica, delete) = query { action }

                delete.foreach(storage.backgroundRmFile)
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

  def deleteReplicas(storage: StorageService): Unit = deleteReplicas(storage.id)
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
