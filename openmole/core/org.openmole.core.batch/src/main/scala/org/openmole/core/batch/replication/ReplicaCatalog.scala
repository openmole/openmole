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
import org.openmole.tool.service.Logger
import java.util.regex.Pattern
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.environment.BatchEnvironment._
import java.util.concurrent.TimeUnit

import org.openmole.tool.lock.LockRepository

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.slick.driver.H2Driver.simple._
import scala.util.{ Success, Failure, Try }

object ReplicaCatalog extends Logger {

  import Log._

  val NoAccessCleanTime = new ConfigurationLocation("ReplicaCatalog", "NoAccessCleanTime")
  val InCatalogCacheTime = new ConfigurationLocation("ReplicaCatalog", "InCatalogCacheTime")
  val ReplicaCacheTime = new ConfigurationLocation("ReplicaCatalog", "ReplicaCacheTime")
  val ReplicaGraceTime = new ConfigurationLocation("ReplicaCatalog", "ReplicaGraceTime")
  val LockTimeout = new ConfigurationLocation("ReplicaCatalog", "LockTimeout")

  Workspace += (NoAccessCleanTime, "P30D")
  Workspace += (InCatalogCacheTime, "PT2M")
  Workspace += (ReplicaCacheTime, "PT30M")
  Workspace += (ReplicaGraceTime, "P1D")
  Workspace += (LockTimeout, "PT1M")

  lazy val replicationPattern = Pattern.compile("(\\p{XDigit}*)_.*")

  lazy val inCatalogCache = new TimeCache[Map[String, Set[String]]]
  lazy val database = {
    val dbInfoFile = DBServerInfo.dbInfoFile
    val info = DBServerInfo.load(dbInfoFile)
    val db = Database.forDriver(
      driver = new org.h2.Driver,
      url = s"jdbc:h2:tcp://localhost:${info.port}/${DBServerInfo.dbDirectory}/${DBServerInfo.urlDBPath}",
      user = info.user,
      password = info.password)
    db.withSession {
      s ⇒
        val statement = s.createStatement()
        statement.execute(s"SET DEFAULT_LOCK_TIMEOUT ${Workspace.preferenceAsDuration(LockTimeout).toMillis}")
    }
    db
  }

  lazy val localLock = new LockRepository[ReplicaCacheKey]

  type ReplicaCacheKey = (String, String, String)
  val replicaCache = CacheBuilder.newBuilder.asInstanceOf[CacheBuilder[ReplicaCacheKey, Replica]].
    expireAfterWrite(Workspace.preferenceAsDuration(ReplicaCacheTime).toSeconds, TimeUnit.SECONDS).build[ReplicaCacheKey, Replica]

  def withSession[T](f: Session ⇒ T) = database.withSession { s ⇒ f(s) }

  def inCatalog(implicit session: Session) = inCatalogCache(inCatalogQuery, Workspace.preferenceAsDuration(InCatalogCacheTime).toMillis)

  private def inCatalogQuery(implicit session: Session): Map[String, Set[String]] =
    replicas.map {
      replica ⇒ (replica.storage, replica.hash)
    }.run.groupBy(_._1).mapValues {
      _.map { case (_, hash) ⇒ hash }.toSet
    }.withDefaultValue(Set.empty)

  def uploadAndGet(
    upload: ⇒ String,
    srcPath: File,
    hash: String,
    storage: StorageService)(implicit token: AccessToken, session: Session): Replica = {

    val cacheKey = (srcPath.getCanonicalPath, hash, storage.id)

    // Avoid same transfer in multiple threads
    localLock.withLock(cacheKey) {
      Option(replicaCache.getIfPresent(cacheKey)) match {
        case Some(r) ⇒ r
        case None ⇒

          //If replica is already present on the storage with another hash
          def cleanOldReplicas = {
            def getReplicasForSrcWithOtherHash =
              replicas.filter { r ⇒
                r.source === srcPath.getCanonicalPath && r.hash =!= hash && r.storage === storage.id
              }

            val samePath = getReplicasForSrcWithOtherHash
            samePath.foreach {
              replica ⇒
                logger.fine(s"Remove obsolete $replica")
                storage.backgroundRmFile(replica.path)
            }
            samePath.delete
          }

          @tailrec def uploadAndInsertIfNotInCatalog: Replica = {
            //Remove deleted replicas
            def stillExists(r: Replica) =
              if (r.lastCheckExists + Workspace.preferenceAsDuration(BatchEnvironment.CheckFileExistsInterval).toMillis < System.currentTimeMillis) {
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

            getReplica.firstOption match {
              case Some(replica) ⇒ assertReplica(replica)
              case None ⇒
                val newFile = upload
                val replica = session.withTransaction {
                  getReplica.firstOption match {
                    case Some(r) ⇒
                      logger.fine("Already in database deleting")
                      storage.backgroundRmFile(newFile)
                      assertReplica(r)
                    case None ⇒
                      val newReplica = Replica(
                        source = srcPath.getCanonicalPath,
                        storage = storage.id,
                        path = newFile,
                        hash = hash,
                        lastCheckExists = System.currentTimeMillis)

                      assert(newReplica.path != null)

                      logger.fine(s"Insert $newReplica")
                      try replicas += newReplica
                      catch {
                        case t: JdbcSQLException ⇒
                          storage.backgroundRmFile(newFile)
                          throw t
                      }
                      assertReplica(newReplica)
                  }
                }

                if (stillExists(replica)) replica
                else {
                  remove(replica.id)
                  uploadAndInsertIfNotInCatalog
                }
            }
          }

          cleanOldReplicas
          val replica = uploadAndInsertIfNotInCatalog
          replicaCache.put(cacheKey, replica)
          replica
      }
    }

  }

  def forPath(path: String)(implicit session: Session) = replicas.filter { _.path === path }
  def onStorage(storage: StorageService)(implicit session: Session) = replicas.filter { _.storage === storage.id }

  def remove(id: Long)(implicit session: Session) = {
    logger.fine(s"Remove replica with id $id")
    replicas.filter { _.id === id }.delete
  }

  def timeOfPersistent(name: String) = {
    val matcher = replicationPattern.matcher(name)
    if (!matcher.matches) None
    else Try(matcher.group(1).toLong).toOption
  }

}
