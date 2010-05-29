/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
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
import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.core.replicacatalog.IReplicaCatalog;
import org.openmole.misc.workspace.IWorkspace;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.Configuration;
import com.db4o.ext.DatabaseFileLockedException;
import com.db4o.ext.DatabaseReadOnlyException;
import com.db4o.ext.Db4oIOException;
import com.db4o.ext.IncompatibleFileFormatException;
import com.db4o.ext.OldFormatException;
import com.db4o.query.Query;
import com.db4o.ta.TransparentPersistenceSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.commons.tools.io.IHash;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.core.replicacatalog.IReplica;
import org.openmole.core.file.GZipedURIFile;
import org.openmole.core.file.URIFile;
import org.openmole.core.file.URIFileCleaner;
import org.openmole.core.workflow.model.execution.IEnvironment;
import org.openmole.core.workflow.model.execution.batch.IAccessToken;
import org.openmole.core.workflow.model.execution.batch.IBatchEnvironmentDescription;
import org.openmole.core.workflow.model.execution.batch.IBatchServiceDescription;
import org.openmole.core.workflow.model.execution.batch.IBatchStorage;


//FIXME when Equinox bug #221329 is resolved remove the synchronized
public class ReplicaCatalog implements IReplicaCatalog {

    static ConfigurationLocation GCUpdateInterval = new ConfigurationLocation(ReplicaCatalog.class.getSimpleName(), "GCUpdateInterval");

    static {
        Activator.getWorkpace().addToConfigurations(GCUpdateInterval, "PT2M");
    }
    final ReplicaLockRepository locks;
    final ObjectContainer objServeur;

    public ReplicaCatalog() throws InternalProcessingError {
        super();
        try {
            String objRepoLocation = Activator.getWorkpace().getPreference(IWorkspace.ObjectRepoLocation);
            objServeur = Db4o.openFile(getB4oConfiguration(), objRepoLocation);
            locks = new ReplicaLockRepository();
            long updateInterval = Activator.getWorkpace().getPreferenceAsDurationInMs(GCUpdateInterval);
            Activator.getUpdater().registerForUpdate(new ReplicaCatalogGC(this, updateInterval), ExecutorType.OWN);
        } catch (Db4oIOException e) {
            throw new InternalProcessingError(e);
        } catch (DatabaseFileLockedException e) {
            throw new InternalProcessingError(e);
        } catch (IncompatibleFileFormatException e) {
            throw new InternalProcessingError(e);
        } catch (OldFormatException e) {
            throw new InternalProcessingError(e);
        } catch (DatabaseReadOnlyException e) {
            throw new InternalProcessingError(e);
        }
    }

    synchronized private Replica getReplica(final IHash hash, final IBatchServiceDescription storageDescription, final IBatchEnvironmentDescription environmentDescription, final boolean zipped) {

        ObjectSet<Replica> set;
        set = objServeur.queryByExample(new Replica(null, hash, storageDescription, environmentDescription, zipped, null));
        if (!set.isEmpty()) {
            return set.get(0);
        }
        return null;

    }

    private synchronized  Replica getReplica(final File src, final IHash hash, final IBatchServiceDescription storageDescription, final IBatchEnvironmentDescription environmentDescription, final boolean zipped)  {

        ObjectSet<Replica> set;

        ObjectContainer objectContainer = objServeur;

        try {

            set = objectContainer.queryByExample(new Replica(src, hash, storageDescription, environmentDescription, zipped, null));

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
                        build.append(rep.toString() + ';');
                    }
                    Logger.getLogger(ReplicaCatalog.class.getName()).log(Level.WARNING, "Replica catalog corrupted (going to be repared), " + set.size() + " records: " + build.toString());

                    replica = fix(set, objectContainer);
            }
            return replica;
        } finally {
            objectContainer.commit();
        }

    }

    synchronized ObjectSet<Replica> getReplica(final File src, final IBatchServiceDescription storageDescription, final IBatchEnvironmentDescription envDescription, final boolean zipped) {
        ObjectSet<Replica> ret = objServeur.queryByExample(new Replica(src, null, storageDescription, envDescription, zipped, null));
        return ret;
    }

    //Synchronization should be achieved outiside the replica for database caching and isolation purposes
    @Override
    public IReplica uploadAndGet(final File src, final IHash hash, final IBatchStorage storage, final boolean zipped, IAccessToken token) throws InternalProcessingError, UserBadDataError, InterruptedException, IOException {

        final ReplicaCatalogKey key = new ReplicaCatalogKey(hash, storage.getDescription(), storage.getExecutionEnvironment().getDescription());

        locks.lock(key);

        Replica replica;
        try {
            IBatchServiceDescription storageDescription = storage.getDescription();
            IBatchEnvironmentDescription environmentDescription = storage.getExecutionEnvironment().getDescription();

            replica = getReplica(src, hash, storageDescription, environmentDescription, zipped);
            if (replica == null) {
                for (Replica toClean : getReplica(src, storageDescription, environmentDescription, zipped)) {
                    clean(toClean);
                }

                IReplica sameContent = getReplica(hash, storageDescription, environmentDescription, zipped);
                if (sameContent != null) {
                    replica = new Replica(src, hash, storageDescription, environmentDescription, zipped, sameContent.getDestination());
                    insert(replica);
                } else {

                    IURIFile newFile;
                    try {
                        newFile = storage.getTmpSpace().newFileInDir("replica", ".rep");
                        if (zipped) {
                            newFile = new GZipedURIFile(newFile);
                        }

                        URIFile.copy(new URIFile(src), newFile, token);

                        replica = new Replica(src, hash, storage.getDescription(), storage.getExecutionEnvironment().getDescription(), zipped, newFile);
                        insert(replica);

                       // Logger.getLogger(ReplicaCatalog.class.getName()).log(Level.INFO, "Upload replica " + replica.toString());


                    } catch (IOException e) {
                        throw new InternalProcessingError(e);
                    }

                }
            }

            //replica.transfert(token);
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

    synchronized  private Replica insert(Replica replica) {

        File srcToInsert = replica.getSource();
        IHash hashToInsert = replica.getSourceHash();
        IBatchServiceDescription storageDescritptionToInsert = replica.getStorageDescription();
        IBatchEnvironmentDescription environmentDescriptionToInsert = replica.getEnvironmentDescription();
        IURIFile destinationToInsert = replica.getDestination();

        ObjectSet<File> srcsInbase = objServeur.queryByExample(srcToInsert);
        if (!srcsInbase.isEmpty()) {
            srcToInsert = srcsInbase.get(0);
        }

        ObjectSet<IHash> hashsInbase = objServeur.queryByExample(hashToInsert);
        if (!hashsInbase.isEmpty()) {
            hashToInsert = hashsInbase.get(0);
        }

        ObjectSet<IBatchServiceDescription> storagesDescritptionInBase = objServeur.queryByExample(storageDescritptionToInsert);
        if (!storagesDescritptionInBase.isEmpty()) {
            storageDescritptionToInsert = storagesDescritptionInBase.get(0);
        }

        ObjectSet<IBatchEnvironmentDescription> environmentDescriptionInBase = objServeur.queryByExample(environmentDescriptionToInsert);
        if (!environmentDescriptionInBase.isEmpty()) {
            environmentDescriptionToInsert = environmentDescriptionInBase.get(0);
        }

        ObjectSet<IURIFile> destinations = objServeur.queryByExample(destinationToInsert);
        if (!destinations.isEmpty()) {
            destinationToInsert = destinations.get(0);
        }

        replica = new Replica(srcToInsert, hashToInsert, storageDescritptionToInsert, environmentDescriptionToInsert, replica.isZipped(), destinationToInsert);

        objServeur.store(replica);
        return replica;


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
        remove(replica);

        if(getReplica(replica.getSourceHash(), replica.getStorageDescription(), replica.getEnvironmentDescription(), replica.isZipped()) == null)
            return Activator.getExecutorService().getExecutorService(ExecutorType.KILL_REMOVE).submit(new URIFileCleaner(replica.getDestination(), false));
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
        //System.out.println("close dbbbbbbbbbb");
        objServeur.close();
    }

    /*@Override
    public File getLocalFileCache(IURIFile src) throws InternalProcessingError, UserBadDataError {
    return getLocalFileCache(src.getLocation());
    }*/
    private Configuration getB4oConfiguration() {
        Configuration configuration = Db4o.newConfiguration();
        configuration.add(new TransparentPersistenceSupport());
        // configuration.add(new TransparentActivationSupport());

        //configuration.objectClass(Replica.class).cascadeOnDelete(true);
        //configuration.objectClass(Replica.class).cascadeOnUpdate(true);
        // configuration.objectClass(Replica.class).cascadeOnActivate(true);

        configuration.objectClass(Replica.class).objectField("source").indexed(true);
        configuration.objectClass(Replica.class).objectField("storageDescription").indexed(true);
        configuration.objectClass(Replica.class).objectField("environmentDescription").indexed(true);
        configuration.objectClass(Replica.class).objectField("persistence").indexed(true);
        configuration.objectClass(Replica.class).objectField("zipped").indexed(true);

        //RandomAccessFileAdapter randomAccessFileAdapter = new RandomAccessFileAdapter();

        //configuration.io(new NonFlushingIoAdapter(randomAccessFileAdapter));

        return configuration;
    }

    @Override
    synchronized public void removeAllReplicaForEnvironment(IEnvironment environment) {

        ObjectContainer container = objServeur;
        try {

            ObjectSet<Replica> set;

            Query q = container.query();
            q.constrain(Replica.class);

            q.descend("environmentDescription").constrain(environment.getDescription());


            set = q.execute();


            for (Replica r : set) {
                remove(r);
            }
        } finally {
            container.commit();
        }

    }
}
