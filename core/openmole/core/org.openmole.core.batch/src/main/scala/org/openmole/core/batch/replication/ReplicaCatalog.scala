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

import com.db4o.ObjectContainer
import com.db4o.ObjectServer
import com.db4o.ObjectSet
import com.db4o.config.ClientServerConfiguration
import com.db4o.cs.Db4oClientServer
import com.db4o.defragment.Defragment
import com.db4o.defragment.DefragmentConfig
import com.db4o.ta.TransparentPersistenceSupport
import com.google.common.cache.CacheBuilder
import java.io.File
import org.openmole.misc.replication.DBServerInfo
import org.openmole.misc.replication.Replica
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.service.LockRepository
import org.openmole.misc.tools.service.Logger
import java.util.regex.Pattern
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.environment.BatchEnvironment._
import org.openmole.misc.tools.service.TimeCache
import org.openmole.misc.updater.Updater
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.hashservice.HashService._
import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._
import scala.collection.immutable.TreeMap
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

object ReplicaCatalog extends Logger {

  val NoAccessCleanTime = new ConfigurationLocation("ReplicaCatalog", "NoAccessCleanTime")
  val InCatalogCacheTime = new ConfigurationLocation("ReplicaCatalog", "InCatalogCacheTime")
  val ReplicaCacheTime = new ConfigurationLocation("ReplicaCatalog", "ReplicaCacheTime")
  val SocketTimeout = new ConfigurationLocation("ReplicaCatalog", "SocketTimeout")

  Workspace += (NoAccessCleanTime, "P30D")
  Workspace += (InCatalogCacheTime, "PT2M")
  Workspace += (ReplicaCacheTime, "PT30M")
  Workspace += (SocketTimeout, "PT10M")

  lazy val replicationPattern = Pattern.compile("(\\p{XDigit}*)_.*")
  lazy val inCatalogCache = new TimeCache[Map[String, Set[String]]]

  type ReplicaCacheKey = (String, String, String, String)
  val replicaCache = CacheBuilder.newBuilder.asInstanceOf[CacheBuilder[ReplicaCacheKey, Replica]].
    expireAfterWrite(Workspace.preferenceAsDuration(ReplicaCacheTime).toSeconds, TimeUnit.SECONDS).build[ReplicaCacheKey, Replica]

  def openClient = {
    val dbInfoFile = DBServerInfo.dbInfoFile(DBServerInfo.base)
    val info = DBServerInfo.load(dbInfoFile)

    val configuration = Db4oClientServer.newClientConfiguration
    configuration.common.add(new TransparentPersistenceSupport)
    configuration.common.objectClass(classOf[Replica]).cascadeOnDelete(true)
    configuration.prefetchObjectCount(1000)
    configuration.common.bTreeNodeSize(256)
    configuration.common.objectClass(classOf[Replica]).objectField("_hash").indexed(true)
    configuration.common.objectClass(classOf[Replica]).objectField("_storage").indexed(true)
    configuration.common.objectClass(classOf[Replica]).objectField("_path").indexed(true)
    configuration.common.objectClass(classOf[Replica]).objectField("_hash").indexed(true)
    configuration.common.objectClass(classOf[Replica]).objectField("_environment").indexed(true)
    configuration.timeoutClientSocket(Workspace.preferenceAsDuration(SocketTimeout).toMilliSeconds.toInt)

    Db4oClientServer.openClient(configuration, "localhost", info.port, info.user, info.password)
  }

  def withClient[T](f: ObjectContainer ⇒ T) = {
    val client = openClient
    try f(client)
    finally client.close
  }

  /*private var _dbInfo: Option[(DBServerInfo, Long)] = None

   def dbInfo = synchronized {
   val dbInfoFile = DBServerInfo.dbInfoFile(DBServerInfo.base)

   if (!dbInfoFile.exists) throw new InternalProcessingError("Database server not launched, file " + dbInfoFile + " doesn't exists.")

   _dbInfo match {
   case Some((server, modif)) if (modif >= dbInfoFile.lastModification) ⇒ server
   case _ ⇒
   val dbInfo = DBServerInfo.load(dbInfoFile) -> dbInfoFile.lastModification
   _dbInfo = Some(dbInfo)
   dbInfo._1
   }
   }*/

  def inCatalog(environment: String)(implicit objectContainer: ObjectContainer) = inCatalogCache(inCatalogQuery(environment), Workspace.preferenceAsDuration(InCatalogCacheTime).toMilliSeconds)

  private def inCatalogQuery(environment: String)(implicit objectContainer: ObjectContainer): Map[String, Set[String]] =
    objectContainer.queryByExample[Replica](new Replica(_environment = environment)).map {
      replica ⇒ replica.hash -> replica.storage
    }.groupBy(_._1).map { case (k, v) ⇒ k -> v.unzip._2.toSet }

  private def key(hash: String, storage: String, environment: String): String = hash + "_" + storage + "_" + environment
  private def key(r: Replica): String = key(r.hash, r.storage, r.environment)
  private def key(hash: String, storage: StorageService): String = key(hash, storage.id, storage.environment.id)

  def withSemaphore[T](key: String, objectContainer: ObjectContainer)(op: ⇒ T) = {
    objectContainer.ext.setSemaphore(key, Int.MaxValue)
    try op
    finally objectContainer.ext.releaseSemaphore(key)
  }

  def uploadAndGet(
    src: File,
    srcPath: File,
    hash: String,
    storage: StorageService)(implicit token: AccessToken, client: ObjectContainer): Replica = {
    withSemaphore(key(hash, storage), client) {
      val environment = storage.environment
      val cacheKey = (srcPath.getCanonicalPath, hash, storage.id, environment.id)

      def getReplicasForSrc(src: File): ObjectSet[Replica] =
        client.queryByExample(new Replica(_source = src.getCanonicalPath, _storage = storage.id, _environment = environment.id))

      def getReplica(src: File, hash: String): Option[Replica] = {
        val set = client.queryByExample(new Replica(_source = src.getCanonicalPath, _storage = storage.id, _hash = hash, _environment = environment.id))

        set.size match {
          case 0 ⇒ None
          case 1 ⇒ Some(set.get(0))
          case _ ⇒ Some(fix(set))
        }
      }

      def getReplicaForHash(hash: String)(implicit objectContainer: ObjectContainer): Option[Replica] = {
        val set = objectContainer.queryByExample(new Replica(_storage = storage.id, _hash = hash, _environment = environment.id))
        if (!set.isEmpty) Some(set.get(0)) else None
      }

      Option(replicaCache.getIfPresent(cacheKey)) match {
        case None ⇒
          val replica = getReplica(srcPath, hash) match {
            case None ⇒
              //If replica is already present on the storage with another hash
              getReplicasForSrc(srcPath).foreach { r ⇒ clean(r, storage) }
              getReplicaForHash(hash) match {
                case Some(sameContent) ⇒
                  val replica = checkExists(sameContent, src, srcPath, storage)
                  val newReplica = new Replica(_source = srcPath.getCanonicalPath, _storage = storage.id, _path = replica.path, _hash = hash, _environment = environment.id, _lastCheckExists = replica.lastCheckExists)
                  insert(newReplica)
                  newReplica
                case None ⇒
                  uploadAndInsert(src, srcPath, hash, storage)
              }
            case Some(r) ⇒
              checkExists(r, src, srcPath, storage)
          }
          replicaCache.put(cacheKey, replica)
          replica
        case Some(r) ⇒ r
      }
    }
  }

  private def checkExists(
    replica: Replica,
    src: File,
    srcPath: File,
    storage: StorageService)(implicit token: AccessToken, objectContainer: ObjectContainer) =
    if (replica.lastCheckExists + Workspace.preferenceAsDuration(BatchEnvironment.CheckFileExistsInterval).toMilliSeconds < System.currentTimeMillis) {
      if (storage.exists(replica.path)) {
        removeNoLock(replica)
        val toInsert = new Replica(_source = replica.source, _storage = replica.storage, _path = replica.path, _hash = replica.hash, _environment = replica.environment, _lastCheckExists = System.currentTimeMillis)
        insert(toInsert)
        toInsert
      } else {
        removeNoLock(replica)
        uploadAndInsert(src, srcPath, replica.hash, storage)
      }
    } else replica

  private def uploadAndInsert(
    src: File,
    srcPath: File,
    hash: String,
    storage: StorageService)(implicit token: AccessToken, objectContainer: ObjectContainer) = {
    val name = Storage.uniqName(hash, ".rep")
    val newFile = storage.child(storage.persistentDir, name)
    require(replicationPattern.matcher(name).matches)

    logger.fine("Uploading " + src + " to " + newFile)
    signalUpload(
      storage.uploadGZ(src, newFile), newFile, storage)
    logger.fine("Uploaded " + src + " to " + newFile)
    val newReplica = new Replica(_source = srcPath.getCanonicalPath, _storage = storage.id, _path = newFile, _hash = hash, _environment = storage.environment.id, _lastCheckExists = System.currentTimeMillis)
    insert(newReplica)
    newReplica
  }

  private def fix(toFix: Iterable[Replica])(implicit objectContainer: ObjectContainer): Replica = {
    for (rep ← toFix.tail) objectContainer.delete(rep)
    toFix.head
  }

  def replicas(storage: StorageService)(implicit objectContainer: ObjectContainer): Iterable[Replica] =
    objectContainer.queryByExample(new Replica(_storage = storage.id, _environment = storage.environment.id)).toList

  private def insert(replica: Replica)(implicit objectContainer: ObjectContainer) = {
    logger.fine("Insert " + replica)
    objectContainer.store(replica)
  }

  def remove(replica: Replica)(implicit objectContainer: ObjectContainer) =
    withSemaphore(key(replica), objectContainer) {
      removeNoLock(replica)
    }

  private def removeNoLock(replica: Replica)(implicit objectContainer: ObjectContainer) =
    objectContainer.delete(replica)

  def clean(replica: Replica, storage: StorageService)(implicit token: AccessToken, objectContainer: ObjectContainer) =
    withSemaphore(key(replica), objectContainer) {
      removeNoLock(replica)
      logger.fine("Remove " + replica)
      if (!contains(replica.storage, replica.path)) {
        logger.fine("Clean " + replica)
        if (storage.exists(replica.path)) storage.backgroundRmFile(replica.path)
      }
    }

  def rmFileIfNotUsed(storage: StorageService, path: String)(implicit objectContainer: ObjectContainer) = {
    val name = new File(path).getName
    val matcher = replicationPattern.matcher(name)
    if (!matcher.matches) storage.backgroundRmFile(path)
    else {
      val hash = matcher.group(1)
      withSemaphore(key(hash, storage), objectContainer) {
        if (!contains(storage.id, path)) storage.backgroundRmFile(path)
      }
    }
  }

  private def contains(storage: String, path: String)(implicit objectContainer: ObjectContainer) =
    !objectContainer.queryByExample(new Replica(_storage = storage, _path = path)).isEmpty

  private def contains(replica: Replica)(implicit objectContainer: ObjectContainer) =
    !objectContainer.queryByExample(replica).isEmpty

  /*def cleanAll(implicit objectContainer: ObjectContainer) =
    for (rep ← allReplicas) clean(rep)*/

}
