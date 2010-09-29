/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.core.implementation.execution.batch;

import java.io.File;
import org.openmole.commons.tools.io.FileInputStream;
import org.openmole.commons.tools.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.openmole.commons.exception.ExecutionException;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.commons.tools.io.IHash;
import org.openmole.commons.tools.io.TarArchiver;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;
import org.openmole.core.model.execution.batch.SampleType;
import org.openmole.core.model.file.IURIFile;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.message.IContextResults;
import org.openmole.core.model.message.IFileMessage;
import org.openmole.core.model.message.IRuntimeResult;
import scala.Tuple2;

/**
 *
 * @author reuillon
 */
public class GetResultFromEnvironment implements Callable<Void> {

    final static Logger LOGGER = Logger.getLogger(GetResultFromEnvironment.class.getName());
    
    final IBatchServiceDescription communicationStorageDescription;
    final IURIFile outputFile;
    final IJob job;
    final BatchEnvironment environment;
    final Long lastStatusChangeInterval;

    public GetResultFromEnvironment(IBatchServiceDescription communicationStorageDescription, IURIFile outputFile, IJob job, BatchEnvironment environment, Long lastStatusChangeInterval) {
        this.communicationStorageDescription = communicationStorageDescription;
        this.outputFile = outputFile;
        this.job = job;
        this.environment = environment;
        this.lastStatusChangeInterval = lastStatusChangeInterval;
    }

    protected void successFullFinish() {
        environment.sample(SampleType.RUNNING, lastStatusChangeInterval, job);
    }

    @Override
    public Void call() throws Exception {

        IAccessToken token = Activator.getBatchRessourceControl().getController(communicationStorageDescription).getUsageControl().waitAToken();

        try {
            File resultFile = outputFile.cache(token);
            IRuntimeResult result;
            try {
                result = Activator.getSerializer().deserialize(resultFile);
            } finally {
                resultFile.delete();
            }
            if (result.getException() != null) {
                LOGGER.log(Level.WARNING, "Fatal exception thrown durring the execution of the job execution on the excution node", result.getException());
            }

            if (result.getStdOut() != null) {
                IFileMessage stdOut = result.getStdOut();

                try {
                    if (!stdOut.isEmpty()) {
                        File stdOutFile = stdOut.getFile().cache(token);
                        try {
                            IHash stdOutHash = Activator.getHashService().computeHash(stdOutFile);
                            if (!stdOutHash.equals(result.getStdOut().getHash())) {
                                LOGGER.log(Level.WARNING, "The standard output has been corrupted durring the transfert.");
                            }

                            synchronized (System.out) {
                                System.out.println("-----------------Output on remote host-----------------");
                                InputStream fis = new FileInputStream(stdOutFile);
                                try {
                                   FileUtil.copy(fis, System.out); 
                                } finally {
                                    fis.close();
                                }
                                System.out.println("-------------------------------------------------------");
                            }
                        } finally {
                            stdOutFile.delete();
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "The standard output transfer has failed.", e);
                }

            } else {
                LOGGER.log(Level.WARNING, "The standard output result was null.");
            }

            if (result.getStdErr() != null) {
                if (!result.getStdErr().isEmpty()) {
                    IURIFile stdErr = result.getStdErr().getFile();
                    try {
                        File stdErrFile = stdErr.cache(token);
                        try {
                            IHash stdErrHash = Activator.getHashService().computeHash(stdErrFile);
                            if (!stdErrHash.equals(result.getStdErr().getHash())) {
                                LOGGER.log(Level.WARNING, "The standard error output has been corrupted durring the transfert.");
                            }
                            
                            synchronized (System.err) {
                                System.err.println("-----------Error output on remote host------------------");
                                InputStream fis = new FileInputStream(stdErrFile);
                                try {
                                  FileUtil.copy(fis, System.err);  
                                } finally {
                                    fis.close();
                                }
                                System.err.println("--------------------------------------------------------");
                            }
                        } finally {
                            stdErrFile.delete();
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "The standard error output transfer has failed.", e);
                    }
                }
            } else {
                LOGGER.log(Level.WARNING, "The standard error result was null.");
            }

            if (result.getTarResult() == null) {
                throw new InternalProcessingError("TarResult uri result was null.");
            }

            Map<File, File> fileReplacement = new TreeMap<File, File>();

            if (!result.getTarResult().isEmpty()) {

                IURIFile tarResult = result.getTarResult().getFile();
                File tarResultFile = tarResult.cache(token);

                try {
                    IHash tarResulHash = Activator.getHashService().computeHash(tarResultFile);
                    if (!tarResulHash.equals(result.getTarResult().getHash())) {
                        throw new InternalProcessingError("Archive has been corrupted durring transfert from the execution environment.");
                    }

                    try {
                        TarArchiveInputStream tis = new TarArchiveInputStream(new FileInputStream(tarResultFile));

                        try {
                            File destDir = Activator.getWorkspace().newDir("tarResult");

                            TarArchiver unZip = new TarArchiver();
                            ArchiveEntry te;

                            while ((te = tis.getNextEntry()) != null) {
                                File dest = new File(destDir, te.getName());                               
                                FileOutputStream os = new FileOutputStream(dest);

                                try {
                                    FileUtil.copy(tis, os);
                                } finally {
                                    os.close();
                                }

                                Tuple2<File, Boolean> fileInfo = result.getFileInfoForEntry(te.getName());
                                if (fileInfo == null) {
                                    throw new InternalProcessingError("Filename not found for entry " + te.getName() + '.');
                                }

                                File file;

                                if (fileInfo._2()) {
                                    file = Activator.getWorkspace().newDir("tarResult");

                                    InputStream destIn = new FileInputStream(dest);
                                    try {
                                        unZip.extractDirArchiveWithRelativePath(file, destIn);
                                    } finally {
                                        destIn.close();
                                    }

                                } else {
                                    file = dest;
                                }

                                fileReplacement.put(fileInfo._1(), file);
                            }

                        } finally {
                            tis.close();
                        }
                    } catch (IOException e) {
                        throw new InternalProcessingError(e);
                    }
                } finally {
                    tarResultFile.delete();
                }
            }

            //Download and deserialize the context results
            IURIFile contextResultsFile = result.getContextResultURI();
            if (contextResultsFile == null) {
                throw new InternalProcessingError("Context results URI was null");
            }

            File contextResutsFileCache = contextResultsFile.cache(token);
            IContextResults contextResults;
            try {
                contextResults = Activator.getSerializer().deserializeReplaceFiles(contextResutsFileCache, fileReplacement);
            } finally {
                contextResutsFileCache.delete();
            }

            int successfull = 0;

            //Try to download the results for all the jobs of the group
            for (IMoleJob moleJob : getJob().getMoleJobs()) {
                if (contextResults.containsResultForJob(moleJob.getId())) {

                    IContext context = contextResults.getContextForJob(moleJob.getId());
                    //  FileMigrator.initFilesInVariables(context, fileCache);

                    synchronized (moleJob) {
                        if (!moleJob.isFinished()) {
                            try {
                                moleJob.rethrowException(context);
                                try {
                                    moleJob.finished(context);
                                    successfull++;
                                } catch (InternalProcessingError e) {
                                    LOGGER.log(Level.SEVERE, "Error when finishing job.", e);
                                } catch (UserBadDataError e) {
                                    LOGGER.log(Level.SEVERE, "Error when finishing job.", e);
                                }

                            } catch (ExecutionException e) {
                                LOGGER.log(Level.WARNING, "Error durring job execution, it will be resubmitted.", e);
                            }
                        }
                    }
                }
            }

            //If sucessfull for full group update stats
            if (successfull == job.size()) {
                successFullFinish();
            }

        } finally {
            Activator.getBatchRessourceControl().getController(communicationStorageDescription).getUsageControl().releaseToken(token);
        }
        return null;
    }

    public IJob getJob() {
        return job;
    }
}
