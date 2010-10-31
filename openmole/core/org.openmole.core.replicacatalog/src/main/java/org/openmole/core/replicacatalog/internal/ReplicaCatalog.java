/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.replicacatalog.internal;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.file.IURIFile;
import org.openmole.core.replicacatalog.IReplicaCatalog;
import org.openmole.misc.workspace.IWorkspace;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.Configuration;
import com.db4o.defragment.Defragment;
import com.db4o.defragment.DefragmentConfig;
import com.db4o.ext.DatabaseFileLockedException;
import com.db4o.ext.DatabaseReadOnlyException;
import com.db4o.ext.Db4oIOException;
import com.db4o.ext.IncompatibleFileFormatException;
import com.db4o.ext.OldFormatException;
import com.db4o.query.Predicate;
import com.db4o.query.Query;
import com.db4o.ta.TransparentPersistenceSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.commons.tools.io.IHash;
import org.openmole.core.file.GZURIFile;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.core.replicacatalog.IReplica;
import org.openmole.core.file.URIFile;
import org.openmole.core.file.URIFileCleaner;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;
import org.openmole.core.model.execution.batch.IBatchStorage;


//FIXME when Equinox bug #221329 is resolved remove the synchronized
public class ReplicaCatalog implements IReplicaCatalog {

    final static Logger LOGGER = Logger.getLogger(ReplicaCatalog.class.getName());

    final static ConfigurationLocation GCUpdateInterval = new ConfigurationLocation(ReplicaCatalog.class.getSimpleName(), "GCUpdateInterval");

    static {
        Activator.getWorkpace().addToConfigurations(GCUpdateInterval, "PT2M");
    }
    final ReplicaLockRepository locks;
    final ObjectContainer objServeur;

    public ReplicaCatalog() throws InternalProcessingError, UserBadDataError {
        super();
        try {
            String objRepoLocation = Activator.getWorkpace().getPreference(IWorkspace.ObjectRepoLocation);
            
            if(new File(objRepoLocation).exists()) {
                DefragmentConfig defragmentConfig = new DefragmentConfig(objRepoLocation);
                defragmentConfig.forceBackupDelete(true);
                Defragment.defrag(defragmentConfig);
            }
            
            objServeur = Db4o.openFile(getB4oConfiguration(), objRepoLocation);
            locks = new ReplicaLockRepository();
            long updateInterval = Activator.getWorkpace().getPreferenceAsDurationInMs(GCUpdateInterval);
            Activator.getUpdater().registerForUpdate(new ReplicaCatalogGC(this), ExecutorType.OWN, updateInterval);
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        } catch (Db4oIOException e) {
            throw new InternalProcessingError(e);
        } catch (DatabaseFileLockedException e) {
            throw new InternalProcessingError(e, "You probably have another instance of OpenMOLE running.");
        } catch (IncompatibleFileFormatException e) {
            throw new InternalProcessingError(e);
        } catch (OldFormatException e) {
            throw new InternalProcessingError(e);
        } catch (DatabaseReadOnlyException e) {
            throw new InternalProcessingError(e);
        }
    }

    synchronized private Replica getReplica(final IHash hash, final IBatchServiceDescription storageDescription, final IBatchServiceAuthenticationKey authenticationKey) {

        ObjectSet<Replica> set;
        set = objServeur.queryByExample(new Replica(null, hash, storageDescription, authenticationKey, null));
        if (!set.isEmpty()) {
            return set.get(0);
        }
        return null;

    }

    private synchronized  Replica getReplica(final File srcPath, final IHash hash, final IBatchServiceDescription storageDescription, final IBatchServiceAuthenticationKey authenticationKey)  {

        ObjectSet<Replica> set;

        ObjectContainer objectContainer = objServeur;

        try {

            set = objectContainer.query(new Predicate<Replica>(Replica.class) {

                @Override
                public boolean match(Replica replica) {
                    return replica.getSource().equals(srcPath) 
                            && replica.getSourceHash().equals(hash) 
                            && replica.getStorageDescription().equals(storageDescription)
                            && replica.getAuthenticationKey().equals(authenticationKey);
                }
                
            });
     
            Replica replica = null;

            switch (set.size()) {
                case 0:
                    break;
                case 1:
                    replica = set.get(0);
                    break;
                default:

                    StringBuilder build = new StringBuilder();
                    for (Replica rep : set) {
                        build.append(rep.toString()).append(';');
                    }
                    LOGGER.log(Level.WARNING, "Replica catalog corrupted (going to be repared), {0} records: {1}", new Object[]{set.size(), build.toString()});

                    replica = fix(set, objectContainer);
            }
            return replica;
        } finally {
            objectContainer.commit();
        }

    }
    

    synchronized ObjectSet<Replica> getReplica(final File src, final IBatchServiceDescription storageDescription, final IBatchServiceAuthenticationKey authenticationKey) {
        ObjectSet<Replica> ret = objServeur.query(new Predicate<Replica>(Replica.class){

            @Override
            public boolean match(Replica replica) {
                return replica.getSource().equals(src)
                        && replica.getStorageDescription().equals(storageDescription)
                        && replica.getAuthenticationKey().equals(authenticationKey);
            }
            
        });
 
        return ret;
    }

    //Synchronization should be achieved outiside the replica for database caching and isolation purposes
    @Override
    public IReplica uploadAndGet(final File src, final File srcPath, final IHash hash, final IBatchStorage storage, final IAccessToken token) throws InternalProcessingError, UserBadDataError, InterruptedException, IOException {

        final ReplicaCatalogKey key = new ReplicaCatalogKey(hash, storage.description(), storage.authenticationKey());

        locks.lock(key);

        Replica replica;
        try {
            IBatchServiceDescription storageDescription = storage.description();
            IBatchServiceAuthenticationKey authenticationKey = storage.authenticationKey();

            replica = getReplica(srcPath, hash, storageDescription, authenticationKey);
        
            
            if (replica == null) {
                                
                for (Replica toClean : getReplica(srcPath, storageDescription, authenticationKey)) {
                    clean(toClean);
                }

                IReplica sameContent = getReplica(hash, storageDescription, authenticationKey);
                if (sameContent != null) {                 
                    replica = new Replica(srcPath, hash, storageDescription, authenticationKey, sameContent.getDestination());
                    insert(replica);
                } else {
                    IURIFile newFile;
                    try {
                        newFile = storage.persistentSpace(token).newFileInDir("replica", ".rep");
                        newFile = new GZURIFile(newFile);                        

                        URIFile.copy(src, newFile, token);

                        replica = new Replica(srcPath, hash, storage.description(), storage.authenticationKey(), newFile);
                        insert(replica);              
                    } catch (IOException e) {
                        throw new InternalProcessingError(e);
                    }
                }
            }
            objServeur.commit();
        } finally {
            locks.unlock(key);
        }

        return replica;
    }

    synchronized  private Replica fix(List<Replica> toFix, ObjectContainer container) {
        Iterator<Replica> fix = toFix.iterator();
        Replica ret = fix.next();

        while (fix.hasNext()) {
            container.delete(fix.next());
        }

        return ret;
    }

    @Override
    synchronized public Iterable<IReplica> getAllReplicas() {
        ObjectContainer container = objServeur;

        Query q = container.query();
        q.constrain(IReplica.class);

        ObjectSet<IReplica> set = q.execute();

        Collection<IReplica> ret = new ArrayList<IReplica>(set.size());
        ret.addAll(set);

        return set;
    }

    synchronized private IReplica insert(final Replica replica) {

        File srcToInsert = replica.getSource();
        IHash hashToInsert = replica.getSourceHash();
        IBatchServiceDescription storageDescritptionToInsert = replica.getStorageDescription();
        IBatchServiceAuthenticationKey authenticationKeyToInsert = replica.getAuthenticationKey();
        IURIFile destinationToInsert = replica.getDestination();

        ObjectSet<File> srcsInbase = objServeur.query(new Predicate<File>(File.class) {

            @Override
            public boolean match(File src) {
                return src.equals(replica.getSource());
            }
            
        });
        
        if (!srcsInbase.isEmpty()) {
            srcToInsert = srcsInbase.get(0);
        }

        ObjectSet<IHash> hashsInbase = objServeur.query(new Predicate<IHash>(IHash.class) {

            @Override
            public boolean match(IHash hash) {
                return hash.equals(replica.getSourceHash());
            }
        });
        
        if (!hashsInbase.isEmpty()) {
            hashToInsert = hashsInbase.get(0);
        }

        ObjectSet<IBatchServiceDescription> storagesDescritptionInBase = objServeur.query(new Predicate<IBatchServiceDescription>(IBatchServiceDescription.class) {

            @Override
            public boolean match(IBatchServiceDescription batchServiceDescription) {
                return batchServiceDescription.equals(replica.getStorageDescription());
            }
        });
        
        if (!storagesDescritptionInBase.isEmpty()) {
            storageDescritptionToInsert = storagesDescritptionInBase.get(0);
        }

        ObjectSet<IBatchServiceAuthenticationKey> authenticationKeyInBase = objServeur.query(new Predicate<IBatchServiceAuthenticationKey>(IBatchServiceAuthenticationKey.class) {

            @Override
            public boolean match(IBatchServiceAuthenticationKey batchEnvironmentDescription) {
                return batchEnvironmentDescription.equals(replica.getAuthenticationKey());
            }
            
        });
        
        if (!authenticationKeyInBase.isEmpty()) {
            authenticationKeyToInsert = authenticationKeyInBase.get(0);
        }

        /*ObjectSet<IURIFile> destinations = objServeur.query(destinationToInsert);
        if (!destinations.isEmpty()) {
            destinationToInsert = destinations.get(0);
        }*/

        final IReplica replicaToInsert = new Replica(srcToInsert, hashToInsert, storageDescritptionToInsert, authenticationKeyToInsert, destinationToInsert);

        objServeur.store(replicaToInsert);
        return replicaToInsert;
    }

    @Override
    synchronized public void remove(final IReplica replica) {

        ObjectContainer container = objServeur;
        try {
            container.delete(replica);
        } finally {
            container.commit();
        }

    }

    synchronized public Future<?> clean(final IReplica replica)  {
        LOGGER.log(Level.FINE, "Cleaning replica {0}", replica.toString());

        remove(replica);

        if(getReplica(replica.getSourceHash(), replica.getStorageDescription(), replica.getAuthenticationKey()) == null)
            return Activator.getExecutorService().getExecutorService(ExecutorType.REMOVE).submit(new URIFileCleaner(new URIFile(replica.getDestination()), false));
        else return null;
    }



    public synchronized List<Future<?>> cleanAll() {
        List<Future<?>> ret = new LinkedList<Future<?>>();

        for (IReplica rep : getAllReplicas()) {

            Future<?> fut = clean(rep);
            if (fut != null) {
                ret.add(fut);
            }

        }

        return ret;
    }

    @Override
    public void close() {
        objServeur.close();
    }

    private Configuration getB4oConfiguration() {
        Configuration configuration = Db4o.newConfiguration();
        configuration.add(new TransparentPersistenceSupport());
        
        configuration.freespace().discardSmallerThan(50);
        
        configuration.objectClass(Replica.class).objectField("hash").indexed(true);
        configuration.objectClass(Replica.class).objectField("source").indexed(true);
        configuration.objectClass(Replica.class).objectField("storageDescription").indexed(true);
        configuration.objectClass(Replica.class).objectField("authenticationKey").indexed(true);
        
        return configuration;
    }

 
}
