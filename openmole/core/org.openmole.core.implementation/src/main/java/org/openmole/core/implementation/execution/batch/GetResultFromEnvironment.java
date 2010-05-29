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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.openmole.commons.exception.ExecutionException;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.io.FastCopy;
import org.openmole.commons.tools.io.IHash;
import org.openmole.commons.tools.io.TarArchiver;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.tools.FileMigrator;
import org.openmole.core.implementation.resource.LocalFileCache;
import org.openmole.core.workflow.model.execution.IJobStatisticCategory;
import org.openmole.core.workflow.model.execution.batch.IAccessToken;
import org.openmole.core.workflow.model.execution.batch.IBatchStorage;
import org.openmole.core.workflow.model.execution.batch.SampleType;
import org.openmole.misc.backgroundexecutor.ITransferable;
import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.job.IJob;
import org.openmole.core.workflow.model.job.IMoleJob;
import org.openmole.core.workflow.model.message.IRuntimeResult;
import org.openmole.core.workflow.model.mole.IExecutionContext;

/**
 *
 * @author reuillon
 */
public class GetResultFromEnvironment implements ITransferable {

    final IBatchStorage communicationStorage;
    final IURIFile outputFile;
    final IJob job;
    final BatchEnvironment environment;
    final IJobStatisticCategory jobStatisticCategory;
    final IExecutionContext executionContext;
    final Long lastStatusChangeInterval;

    public GetResultFromEnvironment(IBatchStorage communicationStorage, IURIFile outputFile, IJob job, BatchEnvironment environment,IJobStatisticCategory jobStatisticCategory, IExecutionContext executionContext,Long lastStatusChangeInterval) {
        this.communicationStorage = communicationStorage;
        this.outputFile = outputFile;
        this.job = job;
        this.environment = environment;
        this.jobStatisticCategory = jobStatisticCategory;
        this.executionContext = executionContext;
        this.lastStatusChangeInterval = lastStatusChangeInterval;
    }

     protected void successFullFinish() {
        environment.sample(SampleType.RUNNING, lastStatusChangeInterval, executionContext, jobStatisticCategory);
     }

    @Override
    public void transfert() throws Throwable {

        IAccessToken token = Activator.getBatchRessourceControl().waitAToken(communicationStorage.getDescription());

        try {
            File resultFile = outputFile.getFile(token);

            IRuntimeResult result = Activator.getMessageSerialiser().loadJarRuntimeResult(resultFile);

            if (result.getException() != null) {
                Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING,"Fatal exception thrown durring the execution of the job execution on the excution node", result.getException());
            }

            if (result.getStdOut() != null) {

                IURIFile stdOut = result.getStdOut().getLeft();

                try {
                    File stdOutFile = stdOut.getFile(token);
                    IHash stdOutHash = Activator.getHashService().computeHash(stdOutFile);
                    if (!stdOutHash.equals(result.getStdOut().getRight())) {
                        Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard output has been corrupted durring the transfert.");
                    }

                    FastCopy.copy(new FileInputStream(stdOutFile), System.out);
                } catch (IOException e) {
                    Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard output transfer has failed.", e);
                }

            } else {
                Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard output result was null.");
            }

            if (result.getStdErr() != null) {
                IURIFile stdErr = result.getStdErr().getLeft();
                try {
                    File stdErrFile = stdErr.getFile(token);
                    IHash stdErrHash = Activator.getHashService().computeHash(stdErrFile);
                    if (!stdErrHash.equals(result.getStdErr().getRight())) {
                        Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard error output has been corrupted durring the transfert.");
                    }

                    FastCopy.copy(new FileInputStream(stdErrFile), System.err);
                } catch (IOException e) {
                    Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard error output transfer has failed.", e);
                }
            } else {
                Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.WARNING, "The standard error result was null.");
            }

            if (result.getTarResult() == null) {
                throw new InternalProcessingError("TarResult uri result was null.");
            }

            IURIFile tarResult = result.getTarResult().getLeft();
            File tarResultFile = tarResult.getFile(token);
            IHash tarResulHash = Activator.getHashService().computeHash(tarResultFile);
            if (!tarResulHash.equals(result.getTarResult().getRight())) {
                throw new InternalProcessingError("Archive has been corrupted durring transfert from the execution environment.");
            }

            LocalFileCache fileCache = new LocalFileCache();

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
                            FastCopy.copy(tis, os);
                        } finally {
                            os.close();
                        }

                        Duo<File, Boolean> fileInfo = result.getFileInfoForEntry(te.getName());
                        if (fileInfo == null) {
                            throw new InternalProcessingError("Filename not found for entry " + te.getName() + '.');
                        }

                        File file;


                        if (fileInfo.getRight()) {
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

                        fileCache.fillInLocalFileCache(fileInfo.getLeft(), file);
                    }
                } finally {
                    tis.close();
                }
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }

            int successfull = 0;
            
            //Try to download the results for all the jobs of the group
            for (IMoleJob moleJob : getJob().getMoleJobs()) {
                if (result.containsResultForJob(moleJob.getId())) {

                    IContext context = result.getContextForJob(moleJob.getId());
                    FileMigrator.initFilesInVariables(context, fileCache);

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

           // Logger.getLogger(GetResultFromEnvironment.class.getName()).log(Level.INFO, successfull + " / " + job.getNbMoleJob());

            //If sucessfull for full group update stats
            if (successfull == job.getNbMoleJob()) {
                   successFullFinish();
            }

        } finally {
            Activator.getBatchRessourceControl().releaseToken(communicationStorage.getDescription(), token);
        }
    }

    public IJob getJob() {
        return job;
    }
}
