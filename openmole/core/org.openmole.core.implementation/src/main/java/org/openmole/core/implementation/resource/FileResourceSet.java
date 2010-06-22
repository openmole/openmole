/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.core.implementation.resource;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.openmole.core.model.resource.ILocalFileCache;
import org.openmole.core.model.resource.IResource;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class FileResourceSet implements IResource {

    Set<FileResource> fileResourceSet = new HashSet<FileResource>();

    @Override
    public void deploy() throws InternalProcessingError, UserBadDataError {
        for(IResource resource: fileResourceSet) {
            resource.deploy();
        }
    }

    @Override
    public void relocate(ILocalFileCache fileCache) throws InternalProcessingError, UserBadDataError {
        for(IResource resource: fileResourceSet) {
            resource.relocate(fileCache);
        }
    }

    @Override
    public Iterable<File> getFiles() throws InternalProcessingError, UserBadDataError {
        Set<File> files = new TreeSet<File>();

        for(IResource resource: fileResourceSet) {
            for(File file: resource.getFiles()) {
                files.add(file);
            }
        }

        return files;
    }


    public void addFileResource(FileResource fileResource) {
        fileResourceSet.add(fileResource);
    }

    public void addFile(File file) {
        fileResourceSet.add(new FileResource(file));
    }

}
