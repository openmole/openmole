/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.replication

import com.db4o.Db4o
import com.db4o.ObjectContainer
import com.db4o.ObjectSet
import com.db4o.config.Configuration
import com.db4o.defragment.Defragment
import com.db4o.defragment.DefragmentConfig
import com.db4o.query.Predicate
import com.db4o.ta.TransparentPersistenceSupport
import java.io.File
import java.net.URI
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.commons.tools.service.IHash
import org.openmole.misc.executorservice.ExecutorType
import org.openmole.core.batch.internal.Activator
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchServiceDescription
import org.openmole.core.batch.control.BatchStorageDescription
import org.openmole.core.batch.environment.BatchAuthenticationKey
import org.openmole.core.batch.environment.BatchAuthenticationKey
import org.openmole.core.batch.environment.BatchStorage
import org.openmole.core.batch.file.GZURIFile
import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.file.URIFileCleaner
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.IWorkspace

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object ReplicaCatalog {
  val LOGGER = Logger.getLogger(ReplicaCatalog.getClass.getName)

  val GCUpdateInterval = new ConfigurationLocation("ReplicaCatalog", "GCUpdateInterval");
  val ObjectRepoLocation = new ConfigurationLocation("ReplicaCatalog", "ObjectRepoLocation")
  Activator.getWorkspace += (GCUpdateInterval, "PT2M")
  Activator.getWorkspace += (ObjectRepoLocation, ".objectRepository.bin")
  
  val locks = new ReplicaLockRepository
  
  lazy val objServeur = {
    val objRepo = Activator.getWorkspace.file(Activator.getWorkspace.preference(ObjectRepoLocation))
            
    if(objRepo.exists) {
      val defragmentConfig = new DefragmentConfig(objRepo.getAbsolutePath)
      defragmentConfig.forceBackupDelete(true);
      Defragment.defrag(defragmentConfig)
    }
            
    Db4o.openFile(dB4oConfiguration, objRepo.getAbsolutePath)
  }
   
  Activator.getUpdater.registerForUpdate(new ReplicaCatalogGC, ExecutorType.OWN, Activator.getWorkspace.preferenceAsDurationInMs(GCUpdateInterval))
 
  private def getReplica(hash: IHash, storageDescription: BatchStorageDescription, authenticationKey: BatchAuthenticationKey): Option[Replica] = synchronized {
    val set = objServeur.queryByExample(new Replica(null, hash, storageDescription, authenticationKey, null));
    if (!set.isEmpty()) Some(set.get(0))
    else None
  }

  private def getReplica(srcPath: File, hash: IHash, storageDescription: BatchStorageDescription,  authenticationKey: BatchAuthenticationKey): Option[Replica] = synchronized {

    val objectContainer = objServeur
    val set = objectContainer.query(new Predicate[Replica](classOf[Replica]) {

        override def `match`(replica: Replica): Boolean = {
          replica.source.equals(srcPath) && replica.hash.equals(hash) && replica.storageDescription.equals(storageDescription) && replica.authenticationKey.equals(authenticationKey)
        }
                
      })
          
    return set.size match {
      case 0 => None
      case 1 => Some(set.get(0))
      case _ =>

        val build = new StringBuilder
        for (rep <- set.iterator) {
          build.append(rep.toString).append(';');
        }
        LOGGER.log(Level.WARNING, "Replica catalog corrupted (going to be repared), {0} records: {1}", Array(set.size, build.toString));

        Some(fix(set, objectContainer))
    }

  }
    

  def getReplica(src: File, storageDescription: BatchStorageDescription, authenticationKey: BatchAuthenticationKey): ObjectSet[Replica] = synchronized {
      
    objServeur.query(new Predicate[Replica](classOf[Replica]){
        
        override def `match`(replica: Replica): Boolean = replica.source.equals(src) && replica.storageDescription.equals(storageDescription) && replica.authenticationKey.equals(authenticationKey)
      
      })
  }

  def isInCatalog(uri: String): Boolean = {
    !objServeur.query(new Predicate[Replica](classOf[Replica]){
        
        override def `match`(replica: Replica): Boolean = replica.destination.location.equals(uri)
      
      }).isEmpty
  }
  
  //Synchronization should be achieved outiside the replica for database caching and isolation purposes
  def uploadAndGet(src: File, srcPath: File, hash: IHash, storage: BatchStorage, token: AccessToken): Replica = {
    LOGGER.log(Level.INFO, "Looking for replica for {0} hash {1}.", Array(srcPath.getAbsolutePath, hash))
    val key = new ReplicaLockKey(hash, storage.description, storage.authenticationKey) 
    locks.lock(key)

    try {
      val storageDescription = storage.description
      val authenticationKey = storage.authenticationKey

      val replica = getReplica(srcPath, hash, storageDescription, authenticationKey) match {
        case None =>
                                
          for (toClean <- getReplica(srcPath, storageDescription, authenticationKey).iterator) clean(toClean)
                
          getReplica(hash, storageDescription, authenticationKey) match {
            case Some(sameContent) => 
              val newReplica = new Replica(srcPath, hash, storageDescription, authenticationKey, sameContent.destination)
              insert(newReplica)
              newReplica
            case None =>
              val newFile = new GZURIFile(storage.persistentSpace(token).newFileInDir("replica", ".rep"))

              URIFile.copy(src, newFile, token)

              val newReplica = new Replica(srcPath, hash, storage.description, storage.authenticationKey, newFile)
              insert(newReplica)
              newReplica 
          }
        case Some(r) => {
            LOGGER.log(Level.INFO, "Found Replica for {0}.", srcPath.getAbsolutePath)
            r
        }
      }
      objServeur.commit         
      replica
    } finally {
      locks.unlock(key)
    }

  }

  private def fix(toFix: Iterable[Replica], container: ObjectContainer): Replica = synchronized {
    for(rep <- toFix.tail) container.delete(rep)
    toFix.head
  }

  def allReplicas:  Iterable[Replica] = synchronized {
    val q = objServeur.query();
    q.constrain(classOf[Replica])

    q.execute.toArray(Array[Replica]())

    /*Collection<IReplica> ret = new ArrayList<IReplica>(set.size());
     ret.addAll(set);

     return set;*/
  }

  private def insert(replica: Replica): Replica = synchronized {

    val srcToInsert = { 
      val srcsInbase = objServeur.query(new Predicate[File](classOf[File]) {
          
          override def `match`(src: File): Boolean = src.equals(replica.source)
          
        })
        
      if (!srcsInbase.isEmpty) srcsInbase.get(0)
      else replica.source
    }

    val hashToInsert = {
      val hashsInbase = objServeur.query(new Predicate[IHash](classOf[IHash]) {
          
          override def `match`(hash: IHash): Boolean = hash.equals(replica.hash)
          
        })
        
      if (!hashsInbase.isEmpty) hashsInbase.get(0)
      else replica.hash
       
    }
        
    val storageDescriptionToInsert =  {
      val storagesDescriptionInBase = objServeur.query(new Predicate[BatchStorageDescription](classOf[BatchStorageDescription]) {
          override def `match`(batchServiceDescription: BatchStorageDescription): Boolean =  batchServiceDescription.equals(replica.storageDescription)
        })
        
      if (!storagesDescriptionInBase.isEmpty) storagesDescriptionInBase.get(0)
      else replica.storageDescription
    }

    val authenticationKeyToInsert = {
      val authenticationKeyInBase = objServeur.query(new Predicate[BatchAuthenticationKey](classOf[BatchAuthenticationKey]) {
          override def `match`(batchEnvironmentDescription: BatchAuthenticationKey): Boolean = batchEnvironmentDescription.equals(replica.authenticationKey)
        })
        
      if (!authenticationKeyInBase.isEmpty)  authenticationKeyInBase.get(0)
      else replica.authenticationKey
    }
         

    /*ObjectSet<IURIFile> destinations = objServeur.query(destinationToInsert);
     if (!destinations.isEmpty()) {
     destinationToInsert = destinations.get(0);
     }*/

    val replicaToInsert = new Replica(srcToInsert, hashToInsert, storageDescriptionToInsert, authenticationKeyToInsert, replica.destination)

    objServeur.store(replicaToInsert)
    replicaToInsert
  }

  def remove(replica: Replica) = synchronized {
    try {
      objServeur.delete(replica)
    } finally {
      objServeur.commit
    }
  }

  def clean(replica: Replica): Option[Future[_]] = synchronized {
    LOGGER.log(Level.FINE, "Cleaning replica {0}", replica.toString)

    remove(replica)

    getReplica(replica.hash, replica.storageDescription, replica.authenticationKey) match {
      case None => None
      case Some(rep) => Some(Activator.getExecutorService.getExecutorService(ExecutorType.REMOVE).submit(new URIFileCleaner(new URIFile(replica.destination)), false))
    }
  }

  def cleanAll: Iterable[Future[_]] = synchronized {
    val ret = new ListBuffer[Future[_]]

    for (rep <- allReplicas) {

      clean(rep) match {
        case None =>
        case Some(fut) => ret += fut
      }
    }

    ret
  }

  def close = objServeur.close

  private def dB4oConfiguration: Configuration = {
    val configuration = Db4o.newConfiguration
    configuration.add(new TransparentPersistenceSupport)
        
    configuration.freespace.discardSmallerThan(50)
        
    configuration.objectClass(classOf[Replica]).objectField("hash").indexed(true)
    configuration.objectClass(classOf[Replica]).objectField("source").indexed(true)
    configuration.objectClass(classOf[Replica]).objectField("storageDescription").indexed(true)
    configuration.objectClass(classOf[Replica]).objectField("authenticationKey").indexed(true)
    configuration.objectClass(classOf[URIFile]).objectField("locatiton").indexed(true)
    
    configuration
  }

}
