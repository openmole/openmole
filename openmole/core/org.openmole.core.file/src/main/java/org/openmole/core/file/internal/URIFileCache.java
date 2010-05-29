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

import org.openmole.commons.tools.filecache.IFileCache;
import java.io.IOException;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.tools.cache.AssociativeCache;
import org.openmole.commons.tools.cache.ICachable;
import org.openmole.core.file.URIFile;
import org.openmole.core.model.execution.batch.IAccessToken;

/**
 *
 * @author reuillon
 */
public class URIFileCache implements IURIFileCache {

    AssociativeCache<URIFile, IFileCache> associativeCache = new AssociativeCache<URIFile, IFileCache>(AssociativeCache.SOFT, AssociativeCache.SOFT);

    @Override
    public IFileCache getURIFileCache(final URIFile uri, Object cacheAssociation) throws InternalProcessingError, InterruptedException {
        return associativeCache.getCache(cacheAssociation, uri, new ICachable<IFileCache>() {

            @Override
            public IFileCache compute() throws InternalProcessingError, InterruptedException {
                try {
                    return uri.cache();
                } catch (IOException ex) {
                    throw  new InternalProcessingError(ex);
                } 
            }

        });
    }

    @Override
    public IFileCache getURIFileCache(final URIFile uri, Object cacheAssociation, final IAccessToken token) throws InternalProcessingError, InterruptedException {
        return associativeCache.getCache(cacheAssociation, uri, new ICachable<IFileCache>() {

            @Override
            public IFileCache compute() throws InternalProcessingError, InterruptedException {
                try {
                    return uri.cache(token);
                } catch (IOException ex) {
                    throw  new InternalProcessingError(ex);
                }
            }

        });
    }



}
