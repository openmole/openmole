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
package org.openmole.runtime;

import java.util.Collection;
import scala.Tuple2;
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication;
import java.io.File;
import org.openmole.commons.tools.io.FileInputStream;
import org.openmole.commons.tools.io.FileOutputStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.tools.function.MapPartialFunction;
import org.openmole.core.file.URIFile;
import org.openmole.core.implementation.message.RuntimeResult;
import org.openmole.core.model.file.IURIFile;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.message.IJobForRuntime;
import org.openmole.core.model.message.IRuntimeResult;
import org.openmole.runtime.internal.Activator;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.commons.tools.io.IHash;
import org.openmole.commons.tools.io.TarArchiver;

import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment;
import org.openmole.core.model.message.IExecutionMessage;
import org.openmole.core.model.message.IReplicatedFile;
import org.openmole.commons.tools.io.StringInputStream;
import org.openmole.commons.tools.service.Priority;
import org.openmole.core.file.GZURIFile;
import org.openmole.core.implementation.message.ContextResults;
import org.openmole.core.implementation.message.FileMessage;
import org.openmole.core.model.message.IContextResults;
import org.openmole.core.model.message.IFileMessage;

import static org.openmole.commons.tools.service.Retry.retry;

public class SimExplorer implements IApplication {

    static int NumberOfLocalTheads = 1;
    static int NbRetry = 3;

    @Override
    public Object start(IApplicationContext context) throws Exception {
        
        String args[] = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

        Options options = new Options();

        options.addOption("a", true, "Path to a serialized authentication to initialize.");
        options.addOption("w", true, "Path for the workspace.");
        options.addOption("i", true, "Path for the input message.");
        options.addOption("o", true, "Path for the output message.");
        options.addOption("p", true, "Path for plugin dir to preload.");
        
        CommandLineParser parser = new BasicParser();
        CommandLine cmdLine;
        
        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            Logger.getLogger(SimExplorer.class.getName()).severe("Error while parsing command line arguments");
            new HelpFormatter().printHelp(" ", options);
            return IApplication.EXIT_OK;
        }
        
        Activator.getWorkspace().setLocation(new File(cmdLine.getOptionValue("w")));

        //init jsaga
        Activator.getJSagaSessionService();

        String environmentPluginDirPath = cmdLine.getOptionValue("p");
        String executionMessageURI = cmdLine.getOptionValue("i");

        File environmentPluginDir = new File(environmentPluginDirPath);
        Activator.getPluginManager().loadDir(environmentPluginDir);
        
        if (cmdLine.hasOption("a")) {
            /* get env and init */
            File envFile = new File(cmdLine.getOptionValue("a"));
            IBatchServiceAuthentication authentication = Activator.getSerialiser().deserialize(envFile);            
            authentication.initialize();
            envFile.delete();
        }

        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;

        File out = Activator.getWorkspace().newFile("openmole", ".out");
        File err = Activator.getWorkspace().newFile("openmole", ".err");

        PrintStream outSt = new PrintStream(out);
        PrintStream errSt = new PrintStream(err);

        System.setOut(outSt);
        System.setErr(errSt);

        
        Throwable exception = null;
        IFileMessage outputMessage = null;
        IFileMessage errorMessage = null;
        IFileMessage tarResultMessage = null;
        Map<String, Tuple2<File, Boolean>> filesInfo = new TreeMap<String, Tuple2<File, Boolean>>();
        IURIFile contextResult = null;
        
        try {
            Activator.getWorkspace().setPreference(LocalExecutionEnvironment.DefaultNumberOfThreads, Integer.toString(NumberOfLocalTheads));
                        
            /*--- get execution message and job for runtime---*/
            Map<File, File> usedFiles = new TreeMap<File, File>();
            
            IURIFile executionMessageFile = new GZURIFile(new URIFile(executionMessageURI));
            File executionMesageFileCache = executionMessageFile.cache();
            IExecutionMessage executionMessage = Activator.getSerialiser().deserialize(executionMesageFileCache);
            executionMesageFileCache.delete();
            
            File pluginDir = Activator.getWorkspace().newDir();

            for (IReplicatedFile plugin : executionMessage.getPlugins()) {
                File replicaFileCache = plugin.getReplica().cache();

                File inPluginDirLocalFile = File.createTempFile("plugin", ".jar", pluginDir);
                replicaFileCache.renameTo(inPluginDirLocalFile);

                if (!Activator.getHashService().computeHash(inPluginDirLocalFile).equals(plugin.getHash())) {
                    throw new InternalProcessingError("Hash of a plugin does't match.");
                }

                usedFiles.put(plugin.getSrc(), inPluginDirLocalFile);
            }


            Activator.getPluginManager().loadDir(pluginDir);


            /* --- Download the files for the local file cache ---*/
            TarArchiver archiver = new TarArchiver();

            for (IReplicatedFile repliURI : executionMessage.getFiles()) {

                //To avoid getting twice the same plugin with different path
                if (!usedFiles.containsKey(repliURI.getSrc())) {

                    File cache = repliURI.getReplica().cache();

                    IHash cacheHash = Activator.getHashService().computeHash(cache);

                    if (!cacheHash.equals(repliURI.getHash())) {
                        throw new InternalProcessingError("Hash is incorrect for file " + repliURI.getSrc().toString() + " replicated at " + repliURI.getReplica().toString());
                    }

                    File local;
                    if (repliURI.isDirectory()) {
                        local = Activator.getWorkspace().newDir("dirReplica");
                        InputStream is = new FileInputStream(cache);

                        try {
                            archiver.extractDirArchiveWithRelativePath(local, is);
                        } finally {
                            is.close();
                        }
                    } else {
                        local = cache;
                    }

                    usedFiles.put(repliURI.getSrc(), local);
                    //fileCache.fillInLocalFileCache(repliURI.getSrc(), local);
                }
            }

            
            IURIFile jobForRuntimeFile = executionMessage.getJobForRuntimeURI().getFile();
            File jobForRuntimeFileCache = jobForRuntimeFile.cache();

            if (!Activator.getHashService().computeHash(jobForRuntimeFileCache).equals(executionMessage.getJobForRuntimeURI().getHash())) {
                throw new InternalProcessingError("Hash of the execution job does't match.");
            }

            IJobForRuntime jobForRuntime = Activator.getSerialiser().deserializeReplaceFiles(jobForRuntimeFileCache, new MapPartialFunction<File, File>(usedFiles));
            jobForRuntimeFileCache.delete();
            
            try {

                /* --- Submit all jobs to the local environment --*/

                AllFinished allFinished = new AllFinished();
                ContextSaver saver = new ContextSaver();

                for (IMoleJob toProcess : jobForRuntime.getMoleJobs()) {
                    Activator.getEventDispatcher().registerListener(toProcess, Priority.HIGH.getValue(), saver, IMoleJob.StateChanged);
                    allFinished.registerJob(toProcess);
                    LocalExecutionEnvironment.getInstance().submit(toProcess);
                }

                allFinished.waitAllFinished();

                IContextResults contextResults = new ContextResults(saver.getResults());
                final File contextResultFile = Activator.getWorkspace().newFile();

                final Tuple2<Collection<File>, Collection<Class>> serializationResult = Activator.getSerialiser().serializeGetPluginClassAndFiles(contextResults, contextResultFile);

                final IURIFile uploadedcontextResults = new GZURIFile(executionMessage.getCommunicationDir().newFileInDir("uplodedTar", ".tgz"));

                retry(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        URIFile.copy(contextResultFile, uploadedcontextResults);
                        return null;
                    }
                }, NbRetry);

                contextResult = uploadedcontextResults;
                contextResultFile.delete();

                /*-- Tar the result files --*/

                final File tarResult = Activator.getWorkspace().newFile("result", ".tar");

                TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(tarResult));
                tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

                
                if (!serializationResult._1.isEmpty()) {
                    
                    try {
                        for (File file : serializationResult._1) {
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
                                toArchive = Activator.getWorkspace().newFile();
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

                            filesInfo.put(entry.getName(), new Tuple2<File, Boolean>(file, file.isDirectory()));
                        }
                    } finally {
                        tos.close();
                    }

                    final IURIFile uploadedTar = new GZURIFile(executionMessage.getCommunicationDir().newFileInDir("uplodedTar", ".tgz"));

                    /*-- Try 3 times to write the result --*/

                    retry(new Callable<Void>() {

                        @Override
                        public Void call() throws Exception {
                            URIFile.copy(tarResult, uploadedTar);
                            return null;
                        }
                    }, NbRetry);

                    tarResultMessage = new FileMessage(uploadedTar, Activator.getHashService().computeHash(tarResult));
                } else {
                    tarResultMessage = FileMessage.EMPTY_RESULT;
                }
                
                tarResult.delete();

            } finally {
                outSt.close();
                errSt.close();

                System.setOut(oldOut);
                System.setErr(oldErr);


                if (out.length() != 0) {
                    IURIFile output = new GZURIFile(executionMessage.getCommunicationDir().newFileInDir("output", ".txt"));
                    URIFile.copy(out, output);
                    outputMessage = new FileMessage(output, Activator.getHashService().computeHash(out));
                } else {
                    outputMessage = FileMessage.EMPTY_RESULT;
                }

                if (err.length() != 0) {                    
                    IURIFile errout = new GZURIFile(executionMessage.getCommunicationDir().newFileInDir("outputError", ".txt"));
                    URIFile.copy(err, errout);
                    errorMessage =  new FileMessage(errout, Activator.getHashService().computeHash(err));
                } else {
                    errorMessage = FileMessage.EMPTY_RESULT;
                }

                out.delete();
                err.delete();
            }
        } catch (Throwable t) {
            exception = t;
        }

        IRuntimeResult runtimeResult = new RuntimeResult(outputMessage, errorMessage, tarResultMessage, filesInfo, exception, contextResult);
        
        final File outputLocal = Activator.getWorkspace().newFile("output", ".res");
        Activator.getSerialiser().serialize(runtimeResult, outputLocal);
        try {
            final IURIFile output = new GZURIFile(new URIFile(cmdLine.getOptionValue("o")));

            retry(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    URIFile.copy(outputLocal, output);
                    return null;
                }
            }, NbRetry);

        } finally {
            outputLocal.delete();
        }
        

        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
    }

}
