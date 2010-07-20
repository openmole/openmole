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

package org.openmole.core.file.internal;

import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.file.IURIFileCache;
import java.io.IOException;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.tools.cache.AssociativeCache;
import org.openmole.commons.tools.cache.ICachable;
import org.openmole.commons.tools.filecache.FileCacheDeleteOnFinalize;
import org.openmole.commons.tools.filecache.IFileCache;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.file.IURIFile;

/**
 *
 * @author reuillon
 */
public class URIFileCache implements IURIFileCache {

    AssociativeCache<IURIFile, IFileCache> associativeCache = new AssociativeCache<IURIFile, IFileCache>(AssociativeCache.WEAK, AssociativeCache.HARD);

    @Override
    public IFileCache getURIFileCache(final IURIFile uri) throws InternalProcessingError, InterruptedException, UserBadDataError {
        return associativeCache.getCache(uri, uri, new ICachable<IFileCache>() {

            @Override
            public IFileCache compute() throws InternalProcessingError, InterruptedException {
                try {
                    return new FileCacheDeleteOnFinalize(uri.cache());
                } catch (IOException ex) {
                    throw  new InternalProcessingError(ex);
                } 
            }

        });
    }

    @Override
    public IFileCache getURIFileCache(final IURIFile uri, final IAccessToken token) throws InternalProcessingError, InterruptedException, UserBadDataError {
        return associativeCache.getCache(uri, uri, new ICachable<IFileCache>() {

            @Override
            public IFileCache compute() throws InternalProcessingError, InterruptedException {
                try {
                    return new FileCacheDeleteOnFinalize(uri.cache(token));
                } catch (IOException ex) {
                    throw  new InternalProcessingError(ex);
                }
            }

        });
    }
    
    @Override
    public void invalidate(final IURIFile uri) {
        associativeCache.invalidateCache(uri, uri);
    }

}
