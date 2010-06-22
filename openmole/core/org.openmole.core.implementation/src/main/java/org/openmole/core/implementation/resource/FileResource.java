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
package org.openmole.core.implementation.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import org.openmole.core.model.resource.ILocalFileCache;
import org.openmole.core.model.resource.IResource;

public class FileResource implements IResource {

    File file;

    public FileResource(File file) {
        super();
        this.file = file;
    }

    public FileResource(String location) {
        this(new File(location));
    }

    @Override
    public void relocate(ILocalFileCache fileCache) throws InternalProcessingError, UserBadDataError {
        if(fileCache.containsCacheFor(file)) {
            file = fileCache.getLocalFileCache(file);
        }
    }

    @Override
    public void deploy() throws InternalProcessingError, UserBadDataError {}

    @Override
    public Iterable<File> getFiles() {
        List<File> ret = new ArrayList<File>(1);
        ret.add(file);
        return ret;
    }

    public File getFile() {
        return file;
    }
    
}
