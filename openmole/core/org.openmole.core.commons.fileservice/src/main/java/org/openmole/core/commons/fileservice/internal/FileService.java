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

package org.openmole.core.commons.fileservice.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.commons.fileservice.IFileService;
import org.openmole.commons.tools.cache.AssociativeCache;
import org.openmole.commons.tools.cache.ICachable;
import org.openmole.commons.tools.filecache.FileCacheDeleteOnFinalize;
import org.openmole.commons.tools.filecache.IFileCache;
import org.openmole.commons.tools.io.IHash;
import org.openmole.commons.tools.io.TarArchiver;

public class FileService implements IFileService {

    AssociativeCache<String, IHash> hashCach = new AssociativeCache<String, IHash>(AssociativeCache.SOFT, AssociativeCache.SOFT);
    AssociativeCache<String, IFileCache> archiveCach = new AssociativeCache<String, IFileCache>(AssociativeCache.SOFT, AssociativeCache.SOFT);


    @Override
    public IHash getHashForFile(File file) throws InternalProcessingError, InterruptedException {
       return getHashForFile(file, file);
    }

    @Override
    public File getArchiveForDir(File file) throws InternalProcessingError, InterruptedException {
        return getArchiveForDir(file, file);
    }

    @Override
    public IHash getHashForFile(final File file, Object cacheLength) throws InternalProcessingError, InterruptedException {
        return hashCach.getCache(cacheLength, file.getAbsolutePath(), new ICachable<IHash>() {

            @Override
            public IHash compute() throws InternalProcessingError, InterruptedException {
                try {
                    return Activator.getHashService().computeHash(file);
                } catch (IOException ex) {
                    throw new InternalProcessingError(ex);
                }
            }
        });
    }

    @Override
    public File getArchiveForDir(final File file, Object cacheLenght) throws InternalProcessingError, InterruptedException {
        return archiveCach.getCache(cacheLenght, file.getAbsolutePath(), new ICachable<IFileCache>() {

            @Override
            public IFileCache compute() throws InternalProcessingError, InterruptedException {
                try {
                    File ret = Activator.getWorkspace().newFile("archive", ".tar");
                    OutputStream os = new FileOutputStream(ret);

                    try {
                        new TarArchiver().createDirArchiveWithRelativePath(file, os);
                    } finally {
                        os.close();
                    }
                    
                    return new FileCacheDeleteOnFinalize(ret);
                } catch (IOException ex) {
                    throw new InternalProcessingError(ex);
                }
            }
        }).getFile();
    }
}


