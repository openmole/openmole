/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.core.implementation.resource;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.TreeBidiMap;
import org.openmole.core.model.resource.ILocalFileCache;
import org.openmole.core.model.resource.IResource;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class FileSetResource implements IResource {

    Set<File> fileSet = new TreeSet<File>();
    transient BidiMap<File, File> originalToDeployed;

    @Override
    public void deploy(ILocalFileCache fileCache) throws InternalProcessingError, UserBadDataError {
        originalToDeployed = new TreeBidiMap<File, File>();
        for(File file : fileSet) {
            File cache = fileCache.getLocalFileCache(file);
            originalToDeployed.put(file, cache);
        }
    }

    @Override
    public Iterable<File> getFiles() {
        return fileSet;
    }

    public Iterable<File> getDeployedFiles() {
        return originalToDeployed.values();
    }

    public File getOriginal(File deployed) {
        return originalToDeployed.getKey(deployed);
    }

    public File getDeployed(File local) {
        return originalToDeployed.get(local);
    }

    public void addFile(File file) {
        fileSet.add(file);
    }

}
