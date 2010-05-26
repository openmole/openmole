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

package org.openmole.core.workflow.implementation.execution.batch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.core.file.URIFile;
import org.openmole.core.workflow.implementation.internal.Activator;
import org.openmole.core.workflow.model.execution.batch.IAccessToken;
import org.openmole.core.workflow.model.execution.batch.IBatchEnvironment;
import org.openmole.core.workflow.model.execution.batch.IBatchJobService;
import org.openmole.core.workflow.model.execution.batch.IBatchService;
import org.openmole.core.workflow.model.execution.batch.IBatchServiceGroup;
import org.openmole.core.workflow.model.execution.batch.IBatchStorage;
import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.misc.updater.IUpdatableFuture;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.core.workflow.implementation.execution.Environment;
import org.openmole.core.workflow.model.execution.IJobStatisticCategory;
import org.openmole.core.workflow.model.execution.batch.IBatchEnvironmentDescription;
import org.openmole.core.workflow.model.execution.batch.IBatchExecutionJob;
import org.openmole.core.workflow.model.execution.batch.IBatchServiceDescription;
import org.openmole.core.workflow.model.job.IJob;
import org.openmole.core.workflow.model.mole.IExecutionContext;

public abstract class BatchEnvironment<JS extends IBatchJobService, DESC extends IBatchEnvironmentDescription> extends Environment<DESC, IBatchExecutionJob> implements IBatchEnvironment<JS, DESC> {

    final static String ConfigGroup = BatchEnvironment.class.getSimpleName();
    final static ConfigurationLocation BestStoragesRatio = new ConfigurationLocation(ConfigGroup, "BestStoragesRatio");
    final static ConfigurationLocation BestJobServiceRatio = new ConfigurationLocation(ConfigGroup, "BestJobServiceRatio");
    final static ConfigurationLocation ResourcesExpulseThreshod = new ConfigurationLocation(ConfigGroup, "ResourcesExpulseThreshod");
    final public static ConfigurationLocation MemorySizeForRuntime = new ConfigurationLocation(ConfigGroup, "MemorySizeForRuntime");

    static {
        Activator.getWorkspace().addToConfigurations(BestStoragesRatio, "1.0");
        Activator.getWorkspace().addToConfigurations(BestJobServiceRatio, "1.0");
        Activator.getWorkspace().addToConfigurations(ResourcesExpulseThreshod, "0.5");
        Activator.getWorkspace().addToConfigurations(MemorySizeForRuntime, "512");
    }
    
    transient BatchServiceGroup<JS> jobServices;
    transient BatchServiceGroup<IBatchStorage> storages;
    transient Lock initJS;
    transient Lock initST;
    transient Map<IBatchServiceDescription, IBatchStorage> storageCache;
    transient Lock storageCacheLock;
    transient Map<IBatchServiceDescription, IBatchService> ressourceCache;
    transient Lock ressourceCacheLock;
    transient Integer memorySizeForRuntime;

    transient boolean isAccessInitialized = false;



     //FIXME potential problems for serialization
    File runtime;

    //final Map<IJob, IGenericTaskCapsule<?, ?>> taskCapsuleForJob = Collections.synchronizedMap(new WeakHashMap<IJob, IGenericTaskCapsule<?, ?>>());

    public BatchEnvironment(DESC description) throws InternalProcessingError {
        super(description);
        memorySizeForRuntime = Activator.getWorkspace().getPreferenceAsInt(MemorySizeForRuntime);
        //Logger.getLogger(BatchEnvironment.class.getName()).info("Initializing " + toString() + " " + getDescription().toString());
        Activator.getUpdater().registerForUpdate(new BatchJobWatcher(this), ExecutorType.OWN);
    }
    
    @Override
    public void submit(IJob job, IExecutionContext executionContext, IJobStatisticCategory statisticCategory) throws InternalProcessingError, UserBadDataError {
        BatchExecutionJob<JS> bej = new BatchExecutionJob<JS>(this, job, executionContext);
        IUpdatableFuture future = Activator.getUpdater().registerForUpdate(bej, ExecutorType.UPDATE);
        bej.setFuture(future);
        getJobRegistries().register(executionContext, statisticCategory, bej);
    }


    protected BatchServiceGroup<IBatchStorage> selectStorages() throws InternalProcessingError, UserBadDataError, InterruptedException {
        initializeAccessIfNeeded();
        final BatchServiceGroup<IBatchStorage> storages = new BatchServiceGroup<IBatchStorage>(Activator.getWorkspace().getPreferenceAsDouble(BestStoragesRatio), Activator.getWorkspace().getPreferenceAsDouble(ResourcesExpulseThreshod));

        Collection<IBatchStorage> stors = allStorages();


        /*for(IBatchStorage storage: stors) {
            Logger.getLogger(BatchEnvironment.class.getName()).info(storage.getURI().toString());
        }*/

        final Semaphore oneFinished = new Semaphore(0);
        final AtomicInteger nbLeftRunning = new AtomicInteger(stors.size());

        for (final IBatchStorage storage : stors) {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        if (storage.test()) {
                           // Logger.getLogger(BatchEnvironment.class.getName()).info("Accepted " + storage.getURI().toString());
                            //System.out.println("Found " + storage.toString());
                            storages.put(storage);
                        }/* else {
                               Logger.getLogger(BatchEnvironment.class.getName()).info("Rejected " + storage.getURI().toString());

                        }*/

                        
                    } finally {
                        nbLeftRunning.decrementAndGet();
                        oneFinished.release();
                    }
                }
            };

            Activator.getExecutorService().getExecutorService(ExecutorType.OWN).submit(r);
        }

        while ((storages.isEmpty()) && nbLeftRunning.get() > 0) {
            try {
                oneFinished.acquire();
            } catch (InterruptedException e) {
                Logger.getLogger(BatchEnvironment.class.getName()).log(Level.INFO, null, e);
            }
        }

        if (storages.isEmpty()) {
            throw new InternalProcessingError("No storage available");
        }
        return storages;
    }

    protected BatchServiceGroup<JS> selectWorkingJobServices() throws InternalProcessingError, UserBadDataError, InterruptedException {
        initializeAccessIfNeeded();
        final BatchServiceGroup<JS> jobServices = new BatchServiceGroup<JS>(Activator.getWorkspace().getPreferenceAsDouble(BestJobServiceRatio), Activator.getWorkspace().getPreferenceAsDouble(ResourcesExpulseThreshod));
        Collection<JS> allJobServices = allJobServices();
        final Semaphore done = new Semaphore(0);
        final AtomicInteger nbStillRunning = new AtomicInteger(allJobServices.size());

        for (final JS js : allJobServices) {

            Runnable test = new Runnable() {

                @Override
                public void run() {
                    try {
                        if (js.test()) {
                            //Logger.getLogger(BatchEnvironment.class.getName()).log(Level.INFO, "Accepted JS " + js.toString());
                            jobServices.put(js);
                        } /*else {
                            Logger.getLogger(BatchEnvironment.class.getName()).log(Level.INFO, "Not accepted JS " + js.toString());
                        }*/
                    } finally {
                        nbStillRunning.decrementAndGet();
                        done.release();
                    }
                }
            };

            Activator.getExecutorService().getExecutorService(ExecutorType.OWN).submit(test);
        }


        while (jobServices.isEmpty() && nbStillRunning.get() > 0) {
            try {
                done.acquire();
            } catch (InterruptedException e) {
                Logger.getLogger(BatchEnvironment.class.getName()).log(Level.INFO, null, e);
            }
        }

        if (jobServices.isEmpty()) {
            throw new InternalProcessingError("No job submission service available");
        }

        return jobServices;
    }

    @Override
    public Duo<IBatchStorage, IAccessToken> getAStorage() throws InternalProcessingError, UserBadDataError, InterruptedException {
        return getStorages().getAService();
    }

    @Override
    public IBatchServiceGroup<JS> getJobServices() throws InternalProcessingError, UserBadDataError, InterruptedException {

        getInitJS().lock();
        try {
            if (jobServices == null || jobServices.isEmpty()) {
                //initialize();
                jobServices = selectWorkingJobServices();
            }
            return jobServices;
        } finally {
            getInitJS().unlock();
        }
    }

    @Override
    public IBatchServiceGroup<IBatchStorage> getStorages() throws InternalProcessingError, UserBadDataError, InterruptedException {
        getInitST().lock();

        try {
            if (storages == null || storages.isEmpty()) {
                //Logger.getLogger(BatchEnvironment.class.getName()).log(Level.INFO, "Initializing storages for " + System.identityHashCode(this));
                storages = selectStorages();
            }
            return storages;
        } finally {
            getInitST().unlock();
        }


    }

    @Override
    public Duo<JS, IAccessToken> getAJobService() throws InternalProcessingError, UserBadDataError, InterruptedException {
            return getJobServices().getAService();
    }

 /*   @Override
    public IBatchService getRessource(IBatchServiceDescription desc) throws InternalProcessingError, UserBadDataError {
        IBatchService ressource = getBatchStorageCache().get(desc);
        if (ressource == null) {
            ressource = getRessourceCache().get(desc);
        }
        if (ressource == null) {
            throw new InternalProcessingError("Ressource with description " + desc.toString() + " doesn't exist.");
        }
        return ressource;
    }

    @Override
    public IBatchStorage getStorage(IBatchStorageDescription desc) throws InternalProcessingError, UserBadDataError {
        IBatchStorage storage = getBatchStorageCache().get(desc);
        if (storage == null) {
            throw new InternalProcessingError("Storage with description " + desc.toString() + " doesn't exist.");
        }
        return storage;
    }*/

    private Map<IBatchServiceDescription, IBatchService> getRessourceCache() throws InternalProcessingError, UserBadDataError {
        if (ressourceCache != null) {
            return ressourceCache;
        }

        getRessourceCacheLock().lock();
        try {

            if (ressourceCache == null) {
                Map<IBatchServiceDescription, IBatchService> ressourceCacheTmp = new HashMap<IBatchServiceDescription, IBatchService>();

                Iterator<JS> it = allJobServices().iterator();

                while (it.hasNext()) {
                    JS js = it.next();
                    IBatchServiceDescription desc = js.getDescription();
                    ressourceCacheTmp.put(desc, js);
                }

                ressourceCache = ressourceCacheTmp;
            }
            return ressourceCache;
        } finally {
            getRessourceCacheLock().unlock();
        }
    }

    private Map<IBatchServiceDescription, IBatchStorage> getBatchStorageCache() throws InternalProcessingError, UserBadDataError {
        if (storageCache != null) {
            return storageCache;
        }

        getStorageCacheLock().lock();

        try {

            if (storageCache == null) {
                Map<IBatchServiceDescription, IBatchStorage> storageCacheTmp = new HashMap<IBatchServiceDescription, IBatchStorage>();


                Iterator<IBatchStorage> itStorage = allStorages().iterator();

                while (itStorage.hasNext()) {
                    IBatchStorage sto = itStorage.next();
                    IBatchServiceDescription desc = sto.getDescription();
                    storageCacheTmp.put(desc, sto);
                }

                storageCache = storageCacheTmp;
            }

            return storageCache;

        } finally {
            getStorageCacheLock().unlock();
        }


    }

    private Lock getInitJS() {
        if (initJS != null) {
            return initJS;
        }

        synchronized (this) {
            if (initJS == null) {
                initJS = new ReentrantLock();
            }
            return initJS;
        }
    }

    private Lock getInitST() {
        if (initST != null) {
            return initST;
        }

        synchronized (this) {
            if (initST == null) {
                initST = new ReentrantLock();
            }
            return initST;
        }
    }

    private Lock getRessourceCacheLock() {
        if (ressourceCacheLock != null) {
            return ressourceCacheLock;
        }

        synchronized (this) {
            if (ressourceCacheLock == null) {
                ressourceCacheLock = new ReentrantLock();
            }
            return ressourceCacheLock;
        }
    }

    private Lock getStorageCacheLock() {
        if (storageCacheLock != null) {
            return storageCacheLock;
        }

        synchronized (this) {
            if (storageCacheLock == null) {
                storageCacheLock = new ReentrantLock();
            }
            return storageCacheLock;
        }
    }


    @Override
    public File getRuntime() throws UserBadDataError {
        if (runtime == null) {
            throw new UserBadDataError("Runtime archive URI has not been set for environment " + toString());
        }

        return runtime;
    }

    public void setRuntime(File runtime) {
        this.runtime = runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = new File(runtime);
    }

    @Override
    public void clean() throws InterruptedException, InternalProcessingError, UserBadDataError {
        initializeAccessIfNeeded();
        Activator.getReplicaCatalog().removeAllReplicaForEnvironment(this);

        Collection<Future> futures = new LinkedList<Future>();

        for (final IBatchStorage storage : allStorages()) {
            try {
                futures.add(clean(storage.getBaseDir()));
            } catch (InternalProcessingError t) {
                Logger.getLogger(BatchEnvironment.class.getName()).log(Level.WARNING, "", t);
            }
        }


        try {
            for (Future f : futures) {
                f.get();
            }
        } catch (ExecutionException e) {
            throw new InternalProcessingError(e);
        }

        for (final IBatchStorage storage : allStorages()) {
            Activator.getExecutorService().removeAndShutDownExecutorService(storage.getDescription().toString(), true);
        }
    }

    private Future clean(final IURIFile file) {
      // Logger.getLogger(BatchEnvironment.class.getName()).log(Level.INFO, "Clean " + file.toString());

        Future future = Activator.getExecutorService().getExecutorService(file.getStorageDescription().toString()).submit(new Runnable() {

            @Override
            public void run() {
               try {
                    if (file.URLRepresentsADirectory()) {
                        Collection<Future> futures = new LinkedList<Future>();

                        for (String child : file.list()) {
                            futures.add(clean(new URIFile(file, child)));
                        }

                        for (Future f : futures) {
                            f.get();
                        }

                        file.remove(false);
                        Logger.getLogger(BatchEnvironment.class.getName()).log(Level.INFO, "Cleaned directory " + file.toString());

                    } else {
                        file.remove(false);
                        Logger.getLogger(BatchEnvironment.class.getName()).log(Level.INFO, "Cleaned " + file.toString());
                    }
                } catch (Throwable t) {
                    Logger.getLogger(BatchEnvironment.class.getName()).log(Level.WARNING, "", t);
                }
            }
        });
        return future;
    }


    public synchronized void initializeAccessIfNeeded() throws UserBadDataError, InternalProcessingError, InterruptedException {
        if(!isAccessInitialized) {
            initializeAccess();
            isAccessInitialized = true;
        }
    }

    @Override
    public boolean isAccessInitialized() {
        return isAccessInitialized;
    }

    public Integer getMemorySizeForRuntime() {
        return memorySizeForRuntime;
    }



    @Cachable
    public File getDescriptionFile() throws InternalProcessingError, InterruptedException {
        try {
            File environmentDescription = Activator.getWorkspace().newFile("envrionmentDescription", ".xml");
            Activator.getEnvironmentDescriptionSerializer().serialize(getDescription(), environmentDescription);
            return environmentDescription;
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }
    }

    protected abstract Collection<JS> allJobServices() throws InternalProcessingError, UserBadDataError;

    protected abstract Collection<IBatchStorage> allStorages() throws InternalProcessingError, UserBadDataError;
}
