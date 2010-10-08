/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

import org.openmole.core.batchservicecontrol.UsageControl;
import org.openmole.core.batchservicecontrol.FailureControl;
import java.io.File;
import org.openmole.commons.tools.io.FileInputStream;
import org.openmole.commons.tools.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.core.batchservicecontrol.BatchStorageDescription;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.core.file.URIFile;
import org.openmole.core.file.URIFileCleaner;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.batch.IBatchStorage;
import org.openmole.core.model.file.IURIFile;
import org.openmole.commons.tools.service.RNG;
import org.openmole.misc.workspace.IWorkspace;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication;
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey;
import org.openmole.misc.filecache.IFileCache;
import org.openmole.misc.workspace.ConfigurationLocation;

public class BatchStorage<ENV extends IBatchEnvironment, AUTH extends IBatchServiceAuthentication> extends BatchService<ENV, AUTH> implements IBatchStorage<ENV, AUTH> {

    final static Logger LOGGER = Logger.getLogger(BatchStorage.class.getName());

    final static ConfigurationLocation TmpDirRemoval = new ConfigurationLocation(BatchStorage.class.getSimpleName(), "TmpDirRemoval");
    final static ConfigurationLocation TmpDirRegenerate = new ConfigurationLocation(BatchStorage.class.getSimpleName(), "TmpDirRegenerate");

    static {
        Activator.getWorkspace().addToConfigurations(TmpDirRemoval, "P30D");
        Activator.getWorkspace().addToConfigurations(TmpDirRegenerate, "P1D");
    }
    public final static String persistent = "persistent/";
    public final static String tmp = "tmp/";
    URI location;
    transient IURIFile baseSpace;
    transient IURIFile tmpSpace;
    transient IURIFile persistentSpace;
    transient Long time;

    public BatchStorage(URI baselocation,  ENV batchEnvironment, IBatchServiceAuthenticationKey<? extends AUTH> key, AUTH authentication, int nbAccess) throws InternalProcessingError, UserBadDataError, InterruptedException {
        super(batchEnvironment, key, authentication, new BatchStorageDescription(baselocation), new UsageControl(nbAccess), new FailureControl());
        this.location = baselocation;
    }

    @Override
    public URI getURI() {
        return location;
    }

    protected void setLocation(URI location) {
        this.location = location;
    }

    @Override
    public synchronized IURIFile getPersistentSpace(IAccessToken token) throws InternalProcessingError, InterruptedException, UserBadDataError {
        if (persistentSpace == null) {
            try {
                persistentSpace = getBaseDir(token).mkdirIfNotExist(persistent, token);
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }
        }
        return persistentSpace;
    }

    @Override
    public synchronized IURIFile getTmpSpace(IAccessToken token) throws InternalProcessingError, UserBadDataError, InterruptedException {
        if (tmpSpace == null || time + Activator.getWorkspace().getPreferenceAsDurationInMs(TmpDirRegenerate) < System.currentTimeMillis()) {
            time = System.currentTimeMillis();

            try {
                IURIFile tmpNoTime = getBaseDir(token).mkdirIfNotExist(tmp, token);

                ExecutorService service = Activator.getExecutorService().getExecutorService(ExecutorType.REMOVE);
                Long removalDate = System.currentTimeMillis() - Activator.getWorkspace().getPreferenceAsDurationInMs(TmpDirRemoval);

                for (String dir : tmpNoTime.list(token)) {
                    IURIFile child = new URIFile(tmpNoTime, dir);
                    if (child.URLRepresentsADirectory()) {
                        try {
                            dir = dir.substring(0, dir.length() - 1);
                            Long timeOfDir = Long.parseLong(dir);

                            if (timeOfDir < removalDate) {
                                 LOGGER.log(Level.FINE, "Removing {0} because it's too old.", dir);
                                service.submit(new URIFileCleaner(child, true, false));
                            }
                        } catch (NumberFormatException ex) {
                            LOGGER.log(Level.FINE, "Removing {0} because it doesn't match a date.", dir);
                            service.submit(new URIFileCleaner(child, true, false));
                        }
                    } else {
                        service.submit(new URIFileCleaner(child, false, false));
                    }
                }

                IURIFile tmpTmpDir = tmpNoTime.mkdirIfNotExist(time.toString(), token);
                tmpSpace = tmpTmpDir;
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }
        }
        return tmpSpace;
    }

    @Override
    public IURIFile getBaseDir(IAccessToken token) throws InternalProcessingError, InterruptedException, UserBadDataError {
        if (baseSpace == null) {
            try {
                IURIFile storeFile = new URIFile(getURI().toString());
                baseSpace = storeFile.mkdirIfNotExist(Activator.getWorkspace().getPreference(IWorkspace.UniqueID) + '/', token);
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }
        }
        return baseSpace;
    }

    @Override
    public boolean test() {

        try {

            IAccessToken token = Activator.getBatchRessourceControl().getController(getDescription()).getUsageControl().waitAToken();

            try {
                final int lenght = 10;

                byte[] rdm = new byte[lenght];

                RNG.getRng().nextBytes(rdm);

                IURIFile testFile = getTmpSpace(token).newFileInDir("test", ".bin");
                File tmpFile = Activator.getWorkspace().newFile("test", ".bin");

                try {
                    //BufferedWriter writter = new BufferedWriter(new FileWriter(tmpFile));
                    FileOutputStream output = new FileOutputStream(tmpFile);
                    try {
                        output.write(rdm);
                    } finally {
                        output.close();
                    }

                    URIFile.copy(tmpFile, testFile, token);
                } finally {
                    tmpFile.delete();
                }

                try {
                    IFileCache fileCache = Activator.getURIFileCache().getURIFileCache(testFile, token);
                    File local = fileCache.getFile(false);
                    FileInputStream input = new FileInputStream(local);
                    byte[] resRdm = new byte[lenght];
                    int nb;

                    try {
                        nb = input.read(resRdm);
                    } finally {
                        input.close();
                    }
                    //String tmp = read.readLine();
                    if (nb == lenght && Arrays.equals(rdm, resRdm)) {
                        return true;
                    }
                } finally {
                    Activator.getExecutorService().getExecutorService(ExecutorType.REMOVE).submit(new URIFileCleaner(testFile, false));
                }
            } finally {
                Activator.getBatchRessourceControl().getController(getDescription()).getUsageControl().releaseToken(token);
            }
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, getURI().toString(), e);
        }
        return false;
    }

    @Override
    public String toString() {
        return location.toString();
    }

    @Override
    public IBatchServiceAuthentication getRemoteAuthentication() {
        return getAuthentication();
    }
    
}
