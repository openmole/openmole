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
            IRuntimeResult result = getRuntimeResult(outputFile, token);

            if (result.getException() != null) {
                throw new InternalProcessingError(result.getException(), "Fatal exception thrown durring the execution of the job execution on the excution node");
            }

            display(result.getStdOut(), "Output", token);
            display(result.getStdErr(), "Error output", token);

            Map<File, File> fileReplacement = getFiles(result.getTarResult(), result.getFilesInfo(), token);

            IContextResults contextResults = getContextResults(result.getContextResultURI(), fileReplacement, token);

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

    private IRuntimeResult getRuntimeResult(IURIFile outputFile, IAccessToken token) throws IOException, InterruptedException, InternalProcessingError {
        File resultFile = outputFile.cache(token);
        try {
            return Activator.getSerializer().deserialize(resultFile);
        } finally {
            resultFile.delete();
        }
    }

    private void display(IFileMessage message, String description, IAccessToken token) throws InterruptedException {
        if (message == null) {
            LOGGER.log(Level.WARNING, "{0} is null.", description);
            return;
        }
        try {
            if (!message.isEmpty()) {
                File stdOutFile = message.getFile().cache(token);
                try {
                    IHash stdOutHash = Activator.getHashService().computeHash(stdOutFile);
                    if (!stdOutHash.equals(message.getHash())) {
                        LOGGER.log(Level.WARNING, "The standard output has been corrupted durring the transfert.");
                    }

                    synchronized (System.out) {
                        System.out.println("-----------------" + description + " on remote host-----------------");
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
            LOGGER.log(Level.WARNING, description + " transfer has failed.", e);
        }
    }

    private Map<File, File> getFiles(IFileMessage tarResult, Map<String, Tuple2<File, Boolean>> filesInfo, IAccessToken token) throws InternalProcessingError, IOException, InterruptedException {
        if (tarResult == null) {
            throw new InternalProcessingError("TarResult uri result is null.");
        }

        Map<File, File> fileReplacement = new TreeMap<File, File>();

        if (!tarResult.isEmpty()) {
            IURIFile tarResultURIFile = tarResult.getFile();
            File tarResultFile = tarResultURIFile.cache(token);

            try {
                IHash tarResulHash = Activator.getHashService().computeHash(tarResultFile);
                if (!tarResulHash.equals(tarResult.getHash())) {
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

                            Tuple2<File, Boolean> fileInfo = filesInfo.get(te.getName());
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
        return fileReplacement;
    }

    private IContextResults getContextResults(IURIFile uriFile, Map<File, File> fileReplacement, IAccessToken token) throws InternalProcessingError, IOException, InterruptedException {

        //Download and deserialize the context results

        if (uriFile == null) {
            throw new InternalProcessingError("Context results URI is null");
        }

        File contextResutsFileCache = uriFile.cache(token);

        try {
            return Activator.getSerializer().deserializeReplaceFiles(contextResutsFileCache, fileReplacement);
        } finally {
            contextResutsFileCache.delete();
        }
    }
}
