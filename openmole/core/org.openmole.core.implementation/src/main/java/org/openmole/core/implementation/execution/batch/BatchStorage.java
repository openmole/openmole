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
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.openmole.core.model.execution.batch.IBatchEnvironmentDescription;
import org.openmole.misc.workspace.ConfigurationLocation;

public class BatchStorage extends BatchService implements IBatchStorage {

    final static ConfigurationLocation TmpDirRemoval = new ConfigurationLocation(BatchStorage.class.getSimpleName(), "TmpDirRemoval");
    final static ConfigurationLocation TmpDirRegenerate = new ConfigurationLocation(BatchStorage.class.getSimpleName(), "TmpDirRegenerate");

    static {
        Activator.getWorkspace().addToConfigurations(TmpDirRemoval, "P7D");
        Activator.getWorkspace().addToConfigurations(TmpDirRegenerate, "P1D");
    }
    public final static String persistent = "persistent/";
    public final static String tmp = "tmp/";
    URI location;
    transient IURIFile baseSpace;
    transient IURIFile tmpSpace;
    transient IURIFile persistentSpace;
    transient Long time;

    public BatchStorage(URI baselocation, IBatchEnvironmentDescription batchEnvironmentDescription, int nbAccess) throws InternalProcessingError {
        super(batchEnvironmentDescription, new BatchStorageDescription(baselocation), new UsageControl(nbAccess), new FailureControl(Activator.getWorkspace().getPreferenceAsInt(HistorySize)));
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
    public synchronized IURIFile getPersistentSpace(IAccessToken token) throws InternalProcessingError, InterruptedException {
        if (persistentSpace == null) {
            try {
                persistentSpace = getBaseDir(token).mkdirIfNotExist(persistent, token);
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }
        }
        return persistentSpace;
    }

    /*  @Override
    public synchronized IURIFile getPersistentSpace(IAccessToken token) throws InternalProcessingError, InterruptedException {
    if (persistentSpace == null) {
    try {
    persistentSpace = getBaseDir().mkdirIfNotExist(persistent, token);
    } catch (IOException e) {
    throw new InternalProcessingError(e);
    }
    }     
    return persistentSpace;
    }*/
    @Override
    public synchronized IURIFile getTmpSpace(IAccessToken token) throws InternalProcessingError, UserBadDataError, InterruptedException {
        if (tmpSpace == null || time + Activator.getWorkspace().getPreferenceAsDurationInMs(TmpDirRegenerate) < System.currentTimeMillis()) {
            time = System.currentTimeMillis();

            try {
                IURIFile tmpNoTime = getBaseDir(token).mkdirIfNotExist(tmp, token);

                ExecutorService service = Activator.getExecutorService().getExecutorService(ExecutorType.KILL_REMOVE);
                Long removalDate = System.currentTimeMillis() - Activator.getWorkspace().getPreferenceAsDurationInMs(TmpDirRemoval);

                for (String dir : tmpNoTime.list(token)) {
                    IURIFile child = new URIFile(tmpNoTime, dir);
                    if (child.URLRepresentsADirectory()) {
                        try {
                            Long timeOfDir = Long.parseLong(dir);
                            if (timeOfDir < removalDate) {
                                service.submit(new URIFileCleaner(child, true));
                            }
                        } catch (NumberFormatException ex) {
                            service.submit(new URIFileCleaner(child, true));
                        }
                    } else {
                        service.submit(new URIFileCleaner(child, false));
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

    /* @Override
    public synchronized IURIFile getTmpSpace(IAccessToken token) throws InternalProcessingError, InterruptedException {
    if (tmpSpace == null) {
    try {
    tmpSpace = getBaseDir().mkdirIfNotExist(tmp, token);
    } catch (IOException e) {
    throw new InternalProcessingError(e);
    }
    }
    return tmpSpace;
    }*/
    @Override
    public IURIFile getBaseDir(IAccessToken token) throws InternalProcessingError, InterruptedException {
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

    /* @Override
    public IURIFile getBaseDir(IAccessToken token) throws InternalProcessingError, InterruptedException {
    if (baseSpace == null) {
    try {
    IURIFile storeFile = new URIFile(getURI().toString());
    baseSpace = storeFile.mkdirIfNotExist(Activator.getWorkspace().getPreference(IWorkspace.UniqueID) + '/', token);
    } catch (IOException e) {
    throw new InternalProcessingError(e);
    }
    }
    return baseSpace;
    }*/
    @Override
    public boolean test() {

        try {

            IAccessToken token = Activator.getBatchRessourceControl().waitAToken(getDescription());

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
                    File local = testFile.getFile(token);
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
                    Activator.getExecutorService().getExecutorService(ExecutorType.KILL_REMOVE).submit(new URIFileCleaner(testFile, false));
                }
            } finally {
                Activator.getBatchRessourceControl().releaseToken(getDescription(), token);

            }
        } catch (Throwable e) {
            Logger.getLogger(BatchStorage.class.getName()).log(Level.FINE, getURI().toString(), e);
        }
        return false;
    }

    /*  @Cachable
    @Override
    public IBatchStorageDescription getDescription() {
    return ;
    }
     */
    @Override
    public String toString() {
        return location.toString();
    }
}
