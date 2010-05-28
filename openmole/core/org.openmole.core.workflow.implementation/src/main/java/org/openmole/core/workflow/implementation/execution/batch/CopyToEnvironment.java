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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.io.IHash;
import org.openmole.core.file.GZipedURIFile;
import org.openmole.core.file.URIFile;
import org.openmole.core.workflow.implementation.internal.Activator;
import org.openmole.core.workflow.implementation.message.ExecutionMessage;
import org.openmole.core.workflow.implementation.message.JobForRuntime;
import org.openmole.core.workflow.implementation.message.ReplicatedFile;
import org.openmole.core.workflow.model.execution.batch.IAccessToken;
import org.openmole.core.workflow.model.execution.batch.IBatchEnvironment;
import org.openmole.core.workflow.model.execution.batch.IBatchStorage;
import org.openmole.core.workflow.model.execution.batch.IRuntime;
import org.openmole.misc.backgroundexecutor.ITransferable;
import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.core.workflow.model.job.IJob;
import org.openmole.core.workflow.model.job.IMoleJob;
import org.openmole.core.workflow.model.message.IExecutionMessage;
import org.openmole.core.workflow.model.message.IJobForRuntime;
import org.openmole.core.workflow.model.message.IReplicatedFile;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.core.workflow.model.resource.IResource;
import org.openmole.core.execution.replicacatalog.IReplica;

/**
 *
 * @author reuillon
 */
class CopyToEnvironment implements ITransferable {

    private IBatchStorage communicationStorage;
    private IURIFile communicationDir;
    private IURIFile inputFile;
    private IURIFile outputFile;
    private IRuntime runtime;
    private boolean finished = false;
    final BatchEnvironment environment;
    final IJob job;
    final IExecutionContext executionContext;

    public CopyToEnvironment(BatchEnvironment environment, IJob job, IExecutionContext executionContext) {
        this.environment = environment;
        this.job = job;
        this.executionContext = executionContext;
    }

    void initCommunication() throws InternalProcessingError, UserBadDataError, InterruptedException, IOException, Throwable {

        Duo<IBatchStorage, IAccessToken> duo = getEnvironment().getAStorage();

        communicationStorage = duo.getLeft();
        IAccessToken token = duo.getRight();

        try {
            communicationDir = communicationStorage.getTmpSpace().mkdir(UUID.randomUUID().toString() + '/', token);

            inputFile = new GZipedURIFile(communicationDir.newFileInDir("job", ".in"));
            outputFile = new GZipedURIFile(communicationDir.newFileInDir("job", ".out"));

            runtime = replicateTheRuntime(token);

            IJobForRuntime jobForRuntime = createJobForRuntime(token);
            IExecutionMessage executionMessage = createExecutionMessage(jobForRuntime, token);

            /* ---- upload the execution message ----*/

            File executionMessageFile = Activator.getWorkspace().newTmpFile("job", ".xml");
            Activator.getMessageSerialiser().saveExecutionMessage(executionMessage, executionMessageFile);

            IURIFile executionMessageURIFile = new URIFile(executionMessageFile);
            URIFile.copy(executionMessageURIFile, inputFile, token);

            executionMessageURIFile.remove(false);
            finished = true;
        } finally {
            Activator.getBatchRessourceControl().releaseToken(communicationStorage.getDescription(), token);
        }
    }

    @Override
    public void transfert() throws Throwable {
        initCommunication();
    }

    public boolean isInitialized() {
        return getCommunicationStorage() != null && getCommunicationDir() != null && getOutputFile() != null && getInputFile() != null;
    }

    public IURIFile getCommunicationDir() {
        return communicationDir;
    }

    public IBatchStorage getCommunicationStorage() {
        return communicationStorage;
    }

    public IURIFile getInputFile() {
        return inputFile;
    }

    public IURIFile getOutputFile() {
        return outputFile;
    }

    public IRuntime getRuntime() {
        return runtime;
    }

    IReplicatedFile toReplicatedFile(File file, IBatchStorage storage, IAccessToken token, boolean zipped, IExecutionContext executionContext) throws InternalProcessingError, InterruptedException, UserBadDataError, IOException {
        boolean isDir = file.isDirectory();
        File toReplicate = file;
        if (isDir) {
            toReplicate = Activator.getFileService().getArchiveForDir(file, executionContext);
        }
        IHash hash = Activator.getFileService().getHashForFile(toReplicate, executionContext);
        IReplica replica = Activator.getReplicaCatalog().uploadAndGet(toReplicate, hash, storage, zipped, token);
        return new ReplicatedFile(file, isDir, hash, replica.getDestination());
    }

    public IBatchEnvironment getEnvironment() {
        return environment;
    }

    public IJob getJob() {
        return job;
    }

    public boolean isFinished() {
        return finished;
    }

    IRuntime replicateTheRuntime(IAccessToken token) throws UserBadDataError, InternalProcessingError, InterruptedException, InternalProcessingError, IOException {
        Collection<IURIFile> environmentPluginReplica = new LinkedList<IURIFile>();
        IURIFile runtimeReplica;

        Iterable<File> environmentPlugins = Activator.getPluginManager().getPluginAndDependanciesForClass(getEnvironment().getClass());
        File runtimeFile = getEnvironment().getRuntime();

        for (File environmentPlugin : environmentPlugins) {
            environmentPluginReplica.add(toReplicatedFile(environmentPlugin, communicationStorage, token, true, executionContext).getReplica());
        }

        runtimeReplica = toReplicatedFile(runtimeFile, communicationStorage, token, false, executionContext).getReplica();

        IURIFile environmentDescription = toReplicatedFile(environment.getDescriptionFile(), communicationStorage, token, true, executionContext).getReplica();
        return new Runtime(runtimeReplica, environmentPluginReplica, environmentDescription);

    }

    IExecutionMessage createExecutionMessage(IJobForRuntime jobForRuntime, IAccessToken token) throws InternalProcessingError, UserBadDataError, InterruptedException, IOException {

        File jobFile = Activator.getWorkspace().newTmpFile("job", ".xml");
        Iterable<Class> extendedClases = Activator.getMessageSerialiser().saveJobForRuntime(jobForRuntime, jobFile);

        IURIFile jobURIFile = new URIFile(jobFile);
        IURIFile jobForRuntimeFile = new GZipedURIFile(communicationDir.newFileInDir("job", ".xml"));

        URIFile.copy(jobURIFile, jobForRuntimeFile, token);
        IHash jobHash = Activator.getHashService().computeHash(jobFile);

        jobURIFile.remove(false);

        Set<File> plugins = new TreeSet<File>();
        List<IReplicatedFile> pluginReplicas = new LinkedList<IReplicatedFile>();

        for (Class c : extendedClases) {
            for (File f : Activator.getPluginManager().getPluginAndDependanciesForClass(c)) {
                plugins.add(f);
            }
        }

        for (File f : plugins) {
            IReplicatedFile replicatedPlugin = toReplicatedFile(f, communicationStorage, token, true, executionContext);
            pluginReplicas.add(replicatedPlugin);
        }

        return new ExecutionMessage(pluginReplicas, new Duo<IURIFile, IHash>(jobForRuntimeFile, jobHash));
    }

    IJobForRuntime createJobForRuntime(IAccessToken token) throws InternalProcessingError, InterruptedException, UserBadDataError, IOException {
        IJobForRuntime jobForRuntime = new JobForRuntime(communicationDir);

        Set<File> inputFiles = new TreeSet<File>();

        for (IMoleJob job : getJob().getMoleJobs()) {
            synchronized (job) {
                if (!job.isFinished()) {
                    for (IResource resource : job.getConsumedRessources()) {
                        for (File file : resource.getFiles()) {
                            inputFiles.add(file);
                        }
                    }
                    for (File f : job.getInputFiles()) {
                        inputFiles.add(f);
                    }
                    jobForRuntime.addMoleJob(job);
                }
            }
        }

        for (File file : inputFiles) {
            jobForRuntime.addConsumedFile(toReplicatedFile(file, communicationStorage, token, true, executionContext));
        }

        return jobForRuntime;

    }
}
