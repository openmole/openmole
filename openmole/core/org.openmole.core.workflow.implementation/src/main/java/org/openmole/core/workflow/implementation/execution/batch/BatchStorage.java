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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.core.batchservicecontrol.BatchStorageDescription;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.core.file.URIFile;
import org.openmole.core.file.URIFileCleaner;
import org.openmole.core.workflow.implementation.internal.Activator;
import org.openmole.core.workflow.model.execution.batch.IBatchEnvironment;
import org.openmole.core.workflow.model.execution.batch.IBatchStorage;
import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.commons.tools.service.RNG;
import org.openmole.misc.workspace.IWorkspace;
import org.openmole.core.workflow.model.execution.batch.IAccessToken;

public class BatchStorage extends BatchService implements IBatchStorage {

    public final static String persistent = "persistent/";
    public final static String tmp = "tmp/";
    public final static Long time = System.currentTimeMillis();
    
    URI location;
    transient IURIFile baseSpace;
    transient IURIFile tmpSpace;
    transient IURIFile persistentSpace;

    public BatchStorage(URI baselocation, IBatchEnvironment<?, ?> executionEnvironment, int nbAccess) throws InternalProcessingError {
        super(executionEnvironment, new BatchStorageDescription(baselocation),  new UsageControl(nbAccess), new FailureControl(historySize));
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
    public synchronized IURIFile getPersistentSpace() throws InternalProcessingError, InterruptedException {
        if (persistentSpace == null) {
            try {
                persistentSpace = getBaseDir().mkdirIfNotExist(persistent);
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }
        }
        return persistentSpace;
    }

    @Override
    public synchronized IURIFile getPersistentSpace(IAccessToken token) throws InternalProcessingError, InterruptedException {
        if (persistentSpace == null) {
            try {
                persistentSpace = getBaseDir().mkdirIfNotExist(persistent, token);
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }
        }
        return persistentSpace;
    }

    @Override
    public synchronized IURIFile getTmpSpace() throws InternalProcessingError, InterruptedException {
        if (tmpSpace == null) {
            try {
                tmpSpace = getBaseDir().mkdirIfNotExist(tmp);
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }
        }
        return tmpSpace;
    }

    @Override
    public synchronized IURIFile getTmpSpace(IAccessToken token) throws InternalProcessingError, InterruptedException {
        if (tmpSpace == null) {
            try {
                tmpSpace = getBaseDir().mkdirIfNotExist(tmp, token);
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }
        }
        return tmpSpace;
    }

    @Override
    public IURIFile getBaseDir() throws InternalProcessingError, InterruptedException {
        if (baseSpace == null) {
            try {
                IURIFile storeFile = new URIFile(getURI().toString());
                baseSpace = storeFile.mkdirIfNotExist(Activator.getWorkspace().getPreference(IWorkspace.UniqueID) + '/');
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }
        }
        return baseSpace;
    }

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

    @Override
    public boolean test() {

        try {
            final int lenght = 10;

            byte[] rdm = new byte[lenght];

            RNG.getRng().nextBytes(rdm);


            IURIFile testFile = getTmpSpace().newFileInDir("test", ".bin");
            File tmpFile = Activator.getWorkspace().newFile("test", ".bin");

            try {
                //BufferedWriter writter = new BufferedWriter(new FileWriter(tmpFile));
                FileOutputStream output = new FileOutputStream(tmpFile);
                try {
                    output.write(rdm);
                } finally {
                    output.close();
                }

                IURIFile tmpEfsFile = new URIFile(tmpFile);
                tmpEfsFile.copy(testFile);
            } finally {
                tmpFile.delete();
            }

            try {
                File local = testFile.getFile();
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
        } catch (Throwable e) {
            Logger.getLogger(BatchStorage.class.getName()).log(Level.INFO, getURI().toString(), e);
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
