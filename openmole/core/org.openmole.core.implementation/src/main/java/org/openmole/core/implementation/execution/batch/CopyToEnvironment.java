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
package org.openmole.core.implementation.execution.batch;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.filecache.IFileCache;
import org.openmole.commons.tools.io.IHash;
import org.openmole.core.file.GZURIFile;
import org.openmole.core.file.URIFile;
import org.openmole.core.implementation.execution.JobRegistry;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.message.ExecutionMessage;
import org.openmole.core.implementation.message.FileMessage;
import org.openmole.core.implementation.message.JobForRuntime;
import org.openmole.core.implementation.message.ReplicatedFile;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchStorage;
import org.openmole.core.model.execution.batch.IRuntime;
import org.openmole.core.model.file.IURIFile;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.message.IExecutionMessage;
import org.openmole.core.model.message.IJobForRuntime;
import org.openmole.core.model.message.IReplicatedFile;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.replicacatalog.IReplica;
import org.openmole.core.serializer.ISerializationResult;
import scala.Tuple2;
/**
 *
 * @author reuillon
 */
class CopyToEnvironment implements Callable<CopyToEnvironmentResult> {

    private final BatchEnvironment environment;
    private final IJob job;

    public CopyToEnvironment(BatchEnvironment environment, IJob job) {
        this.environment = environment;
        this.job = job;
    }

    CopyToEnvironmentResult initCommunication() throws InternalProcessingError, UserBadDataError, InterruptedException, IOException {
        
        Tuple2<IBatchStorage, IAccessToken> storage = getEnvironment().getAStorage();

        final IBatchStorage communicationStorage = storage._1();
        final IAccessToken token = storage._2();

        try {
            final IURIFile communicationDir = communicationStorage.getTmpSpace(token).mkdir(UUID.randomUUID().toString() + '/', token);

            
            final IURIFile inputFile = new GZURIFile(communicationDir.newFileInDir("job", ".in"));
            final IURIFile outputFile = new GZURIFile(communicationDir.newFileInDir("job", ".out"));

            final IRuntime runtime = replicateTheRuntime(token, communicationStorage, communicationDir);

            final IJobForRuntime jobForRuntime = createJobForRuntime(token, communicationStorage, communicationDir);
            final IExecutionMessage executionMessage = createExecutionMessage(jobForRuntime, token, communicationStorage, communicationDir);

            /* ---- upload the execution message ----*/

            File executionMessageFile = Activator.getWorkspace().newFile("job", ".xml");
            Activator.getSerializer().serialize(executionMessage, executionMessageFile);

            IURIFile executionMessageURIFile = new URIFile(executionMessageFile);
            URIFile.copy(executionMessageURIFile, inputFile, token);

            executionMessageFile.delete();
            
            return new CopyToEnvironmentResult(communicationStorage, communicationDir, inputFile, outputFile, runtime);
        } finally {
            Activator.getBatchRessourceControl().getController(communicationStorage.getDescription()).getUsageControl().releaseToken(token);
        }
    }

    @Override
    public CopyToEnvironmentResult call() throws Exception {
        return initCommunication();
    }

    IReplicatedFile toReplicatedFile(File file, IBatchStorage storage, IAccessToken token) throws InternalProcessingError, InterruptedException, UserBadDataError, IOException {
        boolean isDir = file.isDirectory();
        File toReplicate = file;
        File toReplicatePath = file.getAbsoluteFile();
        IMoleExecution moleExecution = JobRegistry.getInstance().getMoleExecutionForJob(job);

        //Hold cache to avoid gc and file deletion
        IFileCache cache = null;
        
        if (isDir) {
            cache = Activator.getFileService().getArchiveForDir(file, moleExecution);
            toReplicate = cache.getFile(false);
        }

        IHash hash = Activator.getFileService().getHashForFile(toReplicate, moleExecution);
        IReplica replica = Activator.getReplicaCatalog().uploadAndGet(toReplicate, toReplicatePath, hash, storage, token);
        return new ReplicatedFile(file, isDir, hash, replica.getDestination());
    }

    public IBatchEnvironment getEnvironment() {
        return environment;
    }

    public IJob getJob() {
        return job;
    }


    IRuntime replicateTheRuntime(IAccessToken token, IBatchStorage communicationStorage, IURIFile communicationDir) throws UserBadDataError, InternalProcessingError, InterruptedException, InternalProcessingError, IOException {
        Collection<IURIFile> environmentPluginReplica = new LinkedList<IURIFile>();

        Iterable<File> environmentPlugins = Activator.getPluginManager().getPluginAndDependanciesForClass(getEnvironment().getClass());
        File runtimeFile = getEnvironment().getRuntime();

        for (File environmentPlugin : environmentPlugins) {
            IReplicatedFile replicatedFile = toReplicatedFile(environmentPlugin, communicationStorage, token);
            IURIFile pluginURIFile = replicatedFile.getReplica();
             
            environmentPluginReplica.add(pluginURIFile);
        }

        IURIFile runtimeReplica = toReplicatedFile(runtimeFile, communicationStorage, token).getReplica();
        
        File authenticationFile = Activator.getWorkspace().newFile("envrionmentAuthentication", ".xml");
        Activator.getSerializer().serialize(communicationStorage.getRemoteAuthentication(), authenticationFile);
        IURIFile authenticationURIFile = new GZURIFile(communicationDir.newFileInDir("authentication", ".xml"));
        URIFile.copy(authenticationFile, authenticationURIFile, token);
        authenticationFile.delete();
        
        return new Runtime(runtimeReplica, environmentPluginReplica, authenticationURIFile);
    }

    IExecutionMessage createExecutionMessage(IJobForRuntime jobForRuntime, IAccessToken token, IBatchStorage communicationStorage, IURIFile communicationDir) throws InternalProcessingError, UserBadDataError, InterruptedException, IOException {

        File jobFile = Activator.getWorkspace().newFile("job", ".xml");
        ISerializationResult serializationResult = Activator.getSerializer().serializeAndGetPluginClassAndFiles(jobForRuntime, jobFile);
        
        IURIFile jobURIFile = new URIFile(jobFile);
        IURIFile jobForRuntimeFile = new GZURIFile(communicationDir.newFileInDir("job", ".xml"));

        URIFile.copy(jobURIFile, jobForRuntimeFile, token);
        IHash jobHash = Activator.getHashService().computeHash(jobFile);

        jobURIFile.remove(false);

        Set<File> plugins = new TreeSet<File>();
        List<IReplicatedFile> pluginReplicas = new LinkedList<IReplicatedFile>();

        
        for (Class c: serializationResult.getClassesFromPlugin()) {
            for (File f : Activator.getPluginManager().getPluginAndDependanciesForClass(c)) {
                plugins.add(f);
            }
        }

        for (File f : plugins) {
            IReplicatedFile replicatedPlugin = toReplicatedFile(f, communicationStorage, token);
            pluginReplicas.add(replicatedPlugin);
        }
        
        List<IReplicatedFile> files = new LinkedList<IReplicatedFile>();
        
        for(File file: serializationResult.getFiles()) {
            files.add(toReplicatedFile(file, communicationStorage, token));
        }

        return new ExecutionMessage(pluginReplicas, files, new FileMessage(jobForRuntimeFile, jobHash), communicationDir);
    }

    IJobForRuntime createJobForRuntime(IAccessToken token, IBatchStorage communicationStorage, IURIFile communicationDir) throws InternalProcessingError, InterruptedException, UserBadDataError, IOException {
       
        List<IMoleJob> jobs = new LinkedList<IMoleJob>();

        for (IMoleJob job : getJob().getMoleJobs()) {
            synchronized (job) {
                if (!job.isFinished()) {
                    jobs.add(job);
                }
            }
        }

        return new JobForRuntime(jobs);
    }
}
