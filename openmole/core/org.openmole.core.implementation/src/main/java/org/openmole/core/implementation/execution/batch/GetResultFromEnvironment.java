/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.core.implementation.execution.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.openmole.commons.tools.filecache.IFileCache;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.commons.tools.io.IHash;
import org.openmole.commons.tools.io.TarArchiver;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.tools.FileMigrator;
import org.openmole.core.implementation.resource.LocalFileCache;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;
import org.openmole.core.model.execution.batch.SampleType;
import org.openmole.core.model.file.IURIFile;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.message.IContextResults;
import org.openmole.core.model.message.IRuntimeResult;
import scala.Tuple2;

/**
 *
 * @author reuillon
 */
public class GetResultFromEnvironment implements Callable<Void> {

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
            File resultFile = outputFile.getFileCache(token).getFile(false);

            IRuntimeResult result = Activator.getSerializer().deserialize(resultFile);

            if (result.getException() != null) {
                Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING,"Fatal exception thrown durring the execution of the job execution on the excution node", result.getException());
            }

            if (result.getStdOut() != null) {

                IURIFile stdOut = result.getStdOut()._1();

                try {
                    File stdOutFile = stdOut.getFileCache(token).getFile(false);
                    IHash stdOutHash = Activator.getHashService().computeHash(stdOutFile);
                    if (!stdOutHash.equals(result.getStdOut()._2())) {
                        Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard output has been corrupted durring the transfert.");
                    }

                    synchronized(System.out) {
                        System.out.println("-----------------Output on remote host-----------------");
                        FileUtil.copy(new FileInputStream(stdOutFile), System.out);
                        System.out.println("-------------------------------------------------------");
                    }
                } catch (IOException e) {
                   Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard output transfer has failed.", e);
                }

            } else {
                Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard output result was null.");
            }

            if (result.getStdErr() != null) {
                IURIFile stdErr = result.getStdErr()._1();
                try {
                    File stdErrFile = stdErr.getFileCache(token).getFile(false);
                    IHash stdErrHash = Activator.getHashService().computeHash(stdErrFile);
                    if (!stdErrHash.equals(result.getStdErr()._2())) {
                        Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard error output has been corrupted durring the transfert.");
                    }
                    synchronized(System.err) {
                        System.err.println("-----------Error output on remote host------------------");
                        FileUtil.copy(new FileInputStream(stdErrFile), System.err);
                        System.err.println("--------------------------------------------------------");
                    }
                } catch (IOException e) {
                    Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard error output transfer has failed.", e);
                }
            } else {
                Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard error result was null.");
            }

            if (result.getTarResult() == null) {
                throw new InternalProcessingError("TarResult uri result was null.");
            }

            IURIFile tarResult = result.getTarResult()._1();
            File tarResultFile = tarResult.getFileCache(token).getFile(false);
            IHash tarResulHash = Activator.getHashService().computeHash(tarResultFile);
            if (!tarResulHash.equals(result.getTarResult()._2())) {
                throw new InternalProcessingError("Archive has been corrupted durring transfert from the execution environment.");
            }

            //LocalFileCache fileCache = new LocalFileCache();
            Map<File, File> fileReplacement = new TreeMap<File, File>();
            
            
            try {

                TarArchiveInputStream tis = new TarArchiveInputStream(new FileInputStream(tarResultFile));

                try {
                    File destDir = Activator.getWorkspace().newTmpDir("tarResult");

                    TarArchiver unZip = new TarArchiver();
                    ArchiveEntry te;

                    while ((te = tis.getNextEntry()) != null) {
                        File dest = new File(destDir, te.getName());
                        dest.deleteOnExit();

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
                            file = Activator.getWorkspace().newTmpDir("tarResult");
                            
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
                        //fileCache.fillInLocalFileCache(fileInfo._1(), file);
                    }
                } finally {
                    tis.close();
                }
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }

            //Download and deserialize the context results
            IURIFile contextResultsFile = result.getContextResultURI();
            if(contextResultsFile == null) throw new InternalProcessingError("Context results URI was null");
            
            IFileCache contextResutsFileCache = contextResultsFile.getFileCache(token);
            
            IContextResults contextResults = Activator.getSerializer().deserializeReplaceFiles(contextResutsFileCache.getFile(false), fileReplacement);
  
           
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
                                    Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.SEVERE, "Error when finishing job.", e);
                                } catch (UserBadDataError e) {
                                    Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.SEVERE, "Error when finishing job.", e);
                                }

                            } catch (ExecutionException e) {
                                Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "Error durring job execution, it will be resubmitted.", e);
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
