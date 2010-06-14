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
package org.openmole.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.file.GZipedURIFile;
import org.openmole.core.file.URIFile;
import org.openmole.core.implementation.tools.FileMigrator;
import org.openmole.core.implementation.message.RuntimeResult;
import org.openmole.core.model.file.IURIFile;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.message.IJobForRuntime;
import org.openmole.core.model.message.IRuntimeResult;
import org.openmole.core.model.job.IContext;
import org.openmole.runtime.internal.Activator;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.io.IHash;
import org.openmole.commons.tools.io.TarArchiver;

import org.openmole.misc.workspace.ForbidenPasswordProvider;
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment;
import org.openmole.core.implementation.resource.LocalFileCache;
import org.openmole.core.model.job.IMoleJobId;
import org.openmole.core.model.message.IExecutionMessage;
import org.openmole.core.model.message.IReplicatedFile;
import org.openmole.commons.tools.io.StringInputStream;
import org.openmole.commons.tools.structure.Priority;
import org.openmole.core.model.execution.batch.IBatchEnvironmentDescription;

import static org.openmole.commons.tools.service.Retry.retry;

public class SimExplorer implements IApplication {

    static int NumberOfLocalTheads = 1;
    static int NbRetry = 3;

    @Override
    public Object start(IApplicationContext context) throws Exception {

        String args[] = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

        if (args.length > 4) {
            Activator.getWorkspace().setLocation(new File(args[4]));
        }

        Activator.getWorkspace().setPasswordProvider(new ForbidenPasswordProvider());

        //init jsaga
        Activator.getJSagaSessionService();

        if (args.length < 3) {
            throw new UserBadDataError(null, usage());
        }

        String environmentDescription = args[0];
        String environmentPluginDirPath = args[1];
        String executionMessageURI = args[2];


        File environmentPluginDir = new File(environmentPluginDirPath);
        Activator.getPluginManager().loadDir(environmentPluginDir);

        /* get env and init */
        IBatchEnvironmentDescription real = Activator.getEnvironmentDescriptionSerializer().deserialize(new GZipedURIFile(new URIFile(new File(environmentDescription))).getFile());
        real.createBatchEnvironmentAuthentication().initializeAccess();
        //real.setConfigurationMode(EnvironmentConfiguration.Remote);


        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;

        File out = Activator.getWorkspace().newFile("openmole", ".out");
        File err = Activator.getWorkspace().newFile("openmole", ".err");

        PrintStream outSt = new PrintStream(out);
        PrintStream errSt = new PrintStream(err);

        System.setOut(outSt);
        System.setErr(errSt);

        IRuntimeResult result = new RuntimeResult();


        try {

            LocalExecutionEnvironment.getInstance().setNbThread(NumberOfLocalTheads);

            /*--- get execution message and job for runtime---*/

            LocalFileCache fileCache = new LocalFileCache();

            IURIFile executionMessageFile = new GZipedURIFile(new URIFile(executionMessageURI));
            IExecutionMessage executionMessage = Activator.getMessageSerialiser().loadExecutionMessage(executionMessageFile.getFile());

            File pluginDir = Activator.getWorkspace().newTmpDir();

            for (IReplicatedFile plugin : executionMessage.getPlugins()) {
                File localFile = plugin.getReplica().getFile();
                File inPluginDirLocalFile = File.createTempFile("plugin", ".jar", pluginDir);

                localFile.renameTo(inPluginDirLocalFile);

                if (!Activator.getHashService().computeHash(inPluginDirLocalFile).equals(plugin.getHash())) {
                    throw new InternalProcessingError("Hash of a plugin does't match.");
                }
                fileCache.fillInLocalFileCache(plugin.getSrc(), inPluginDirLocalFile);
            }

            Activator.getPluginManager().loadDir(pluginDir);

            IURIFile jobForRuntimeFile = executionMessage.getJobForRuntimeURI().getLeft();

            if (!Activator.getHashService().computeHash(jobForRuntimeFile.getFile()).equals(executionMessage.getJobForRuntimeURI().getRight())) {
                throw new InternalProcessingError("Hash of the execution job does't match.");
            }

            IJobForRuntime jobForRuntime = Activator.getMessageSerialiser().loadJobForRuntime(jobForRuntimeFile.getFile());

            try {
                /* --- Download the files for the local file cache ---*/
                TarArchiver archiver = new TarArchiver();


                for (IReplicatedFile repliURI : jobForRuntime.getConsumedFiles()) {

                    //To avoid getting twice the same plugin with different path
                    if (!fileCache.containsCacheFor(repliURI.getSrc())) {

                        File cache = repliURI.getReplica().getFile();
                        IHash cacheHash = Activator.getHashService().computeHash(cache);

                        if (!cacheHash.equals(repliURI.getHash())) {
                            throw new InternalProcessingError("Hash is incorrect for file " + repliURI.getSrc().toString() + " replicated at " + repliURI.getReplica().toString());
                        }

                        File local;
                        if (repliURI.isDirectory()) {
                            local = Activator.getWorkspace().newTmpDir("dirReplica");
                            InputStream is = new FileInputStream(cache);

                            try {
                                archiver.extractDirArchiveWithRelativePath(local, is);
                            } finally {
                                is.close();
                            }
                        } else {
                            local = cache;
                        }

                        fileCache.fillInLocalFileCache(repliURI.getSrc(), local);
                    }
                }


                /* --- Submit all jobs to the local environment --*/

                AllFinished allFinished = new AllFinished();
                ContextSaver saver = new ContextSaver();

                
                for (IMoleJob toProcess : jobForRuntime.getMoleJobs()) {

                    FileMigrator.initFilesInVariables(toProcess.getContext(), fileCache);
                    toProcess.getTask().relocate(fileCache);

                    Activator.getEventDispatcher().registerListener(toProcess, Priority.HIGH.getValue(), saver, IMoleJob.stateChanged);
                    allFinished.registerJob(toProcess);

                    LocalExecutionEnvironment.getInstance().submit(toProcess);
                }

                allFinished.waitAllFinished();

                /*-- Tar the result files --*/

                final File tarResult = Activator.getWorkspace().newTmpFile("result", ".tar");

                TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(tarResult));
                tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

                try {
                    for (File file : saver.getOutFiles()) {

                        StringInputStream is = new StringInputStream(file.getCanonicalPath());

                        IHash hash;
                        try {
                            hash = Activator.getHashService().computeHash(is);
                        } finally {
                            is.close();
                        }
                        TarArchiveEntry entry = new TarArchiveEntry(hash.toString());

                        File toArchive;

                        if (file.isDirectory()) {
                            toArchive = Activator.getWorkspace().newTmpFile();
                            OutputStream outputStream = new FileOutputStream(toArchive);

                            try {
                                archiver.createDirArchiveWithRelativePath(file, outputStream);
                            } finally {
                                outputStream.close();
                            }

                        } else {
                            toArchive = file;
                        }

                        //TarArchiveEntry entry = new TarArchiveEntry(file.getName());
                        entry.setSize(toArchive.length());
                        tos.putArchiveEntry(entry);
                        try {
                            FileUtil.copy(new FileInputStream(toArchive), tos);
                        } finally {
                            tos.closeArchiveEntry();
                        }

                        result.addFileName(entry.getName(), file, file.isDirectory());
                    }
                } finally {
                    tos.close();
                }

                final IURIFile uploadedTar = new GZipedURIFile(jobForRuntime.getCommunicationDir().newFileInDir("uplodedTar", ".tgz"));


                /*-- Try 3 times to write the result --*/
               
                retry(new Callable<Void>(){
                    @Override
                    public Void call() throws Exception {
                        new URIFile(tarResult).copy(uploadedTar);
                        return null;
                    }
                }, NbRetry);    
                   

                result.setTarResult(uploadedTar, Activator.getHashService().computeHash(tarResult));
                tarResult.delete();

                for (Duo<IMoleJobId, IContext> res : saver.getResults()) {
                    result.putResult(res.getLeft(), res.getRight());
                }

            } finally {
                outSt.close();
                errSt.close();

                System.setOut(oldOut);
                System.setErr(oldErr);

                IURIFile output = new GZipedURIFile(jobForRuntime.getCommunicationDir().newFileInDir("output", ".txt"));
                new URIFile(out).copy(output);

                IURIFile errout = new GZipedURIFile(jobForRuntime.getCommunicationDir().newFileInDir("outputError", ".txt"));
                new URIFile(err).copy(errout);

                result.setStdOut(output, Activator.getHashService().computeHash(out));
                result.setStdErr(errout, Activator.getHashService().computeHash(err));

                out.delete();
                err.delete();
            }
        } catch (Throwable t) {
            result.setException(t);
        }

        if (args.length > 3) {
            final File outputLocal = Activator.getWorkspace().newFile("output", ".res");
            Activator.getMessageSerialiser().saveRuntimeResult(result, outputLocal);
            try {
                final IURIFile output = new GZipedURIFile(new URIFile(args[3]));

                retry(new Callable<Void>(){
                    @Override
                    public Void call() throws Exception {
                        URIFile.copy(outputLocal, output);
                        return null;
                    }
                }, NbRetry);

            } finally {
                outputLocal.delete();
            }
        }

        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
    }

    private String usage() {
        StringBuffer buf = new StringBuffer();
        buf.append("SimExplorer environment_description execution_plugin_dir URL_of_execution_message [URL_of_output_message] [workspace_location]");
        return buf.toString();
    }
}
