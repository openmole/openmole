/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.misc.fileservice.internal;

import java.io.File;
import org.openmole.commons.tools.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.fileservice.IFileService;
import org.openmole.commons.tools.cache.AssociativeCache;
import org.openmole.commons.tools.cache.ICachable;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.commons.tools.io.IHash;
import org.openmole.commons.tools.io.TarArchiver;
import org.openmole.misc.filecache.FileCacheDeleteOnFinalize;
import org.openmole.misc.filecache.IFileCache;

public class FileService implements IFileService {

    class CachedArchiveForDir extends FileCacheDeleteOnFinalize {

        final long lastModified;

        public CachedArchiveForDir(File file, long lastModified) {
            super(file);
            this.lastModified = lastModified;
        }

        public long getLastModified() {
            return lastModified;
        }
    }

    class HashWithLastModified {

        final IHash hash;
        final long lastModified;

        public HashWithLastModified(IHash hash, long lastModified) {
            this.hash = hash;
            this.lastModified = lastModified;
        }

        public IHash getHash() {
            return hash;
        }

        public long getLastModified() {
            return lastModified;
        }
    }
    
    AssociativeCache<String, HashWithLastModified> hashCach = new AssociativeCache<String, HashWithLastModified>(AssociativeCache.SOFT, AssociativeCache.SOFT);
    AssociativeCache<String, CachedArchiveForDir> archiveCache = new AssociativeCache<String, CachedArchiveForDir>(AssociativeCache.SOFT, AssociativeCache.SOFT);

    @Override
    public IHash getHashForFile(File file) throws InternalProcessingError, UserBadDataError, InterruptedException {
        return getHashForFile(file, file);
    }

    @Override
    public IFileCache getArchiveForDir(File file) throws InternalProcessingError, InterruptedException, UserBadDataError {
        return getArchiveForDir(file, file);
    }

    @Override
    public IHash getHashForFile(final File file, final Object cacheLength) throws InternalProcessingError, UserBadDataError, InterruptedException {
        invalidateHashCacheIfModified(file, cacheLength);
        return hashCach.getCache(cacheLength, file.getAbsolutePath(), new ICachable<HashWithLastModified>() {

            @Override
            public HashWithLastModified compute() throws InternalProcessingError, InterruptedException {
                try {
                    return new HashWithLastModified(Activator.getHashService().computeHash(file), file.lastModified());
                } catch (IOException ex) {
                    throw new InternalProcessingError(ex);
                }
            }
        }).getHash();
    }

    private void invalidateHashCacheIfModified(final File file, final Object cacheLength) {
        HashWithLastModified hashWithLastModified = hashCach.getCached(cacheLength, file.getAbsolutePath());
        if (hashWithLastModified == null) {
            return;
        }

        if (hashWithLastModified.getLastModified() < file.lastModified()) {
            hashCach.invalidateCache(cacheLength, file.getAbsolutePath());
        }
    }

    @Override
    public IFileCache getArchiveForDir(final File file, Object cacheLenght) throws InternalProcessingError, InterruptedException, UserBadDataError {

        invalidateDirCacheIfModified(file, cacheLenght);

        return archiveCache.getCache(cacheLenght, file.getAbsolutePath(), new ICachable<CachedArchiveForDir>() {

            @Override
            public CachedArchiveForDir compute() throws InternalProcessingError, InterruptedException, UserBadDataError {
                try {
                    File ret = Activator.getWorkspace().newFile("archive", ".tar");
                    OutputStream os = new FileOutputStream(ret);

                    try {
                        new TarArchiver().createDirArchiveWithRelativePathNoVariableContent(file, os);
                    } finally {
                        os.close();
                    }

                    return new CachedArchiveForDir(ret, FileUtil.getLastModification(file));
                } catch (IOException ex) {
                    throw new InternalProcessingError(ex);
                }
            }
        });
    }

    private void invalidateDirCacheIfModified(final File file, Object cacheLenght) {
        CachedArchiveForDir cached = archiveCache.getCached(cacheLenght, file.getAbsolutePath());
        if (cached == null) {
            return;
        }

        if (cached.getLastModified() < FileUtil.getLastModification(file)) {
            archiveCache.invalidateCache(cacheLenght, file.getAbsolutePath());
        }
    }
}


