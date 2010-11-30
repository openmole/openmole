/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.file

import org.openmole.commons.tools.cache.AssociativeCache
import org.openmole.core.batch.control.AccessToken
import org.openmole.misc.filecache.FileCacheDeleteOnFinalize
import org.openmole.misc.filecache.IFileCache

object URIFileCache {

    val associativeCache = new AssociativeCache[IURIFile, IFileCache](AssociativeCache.WEAK, AssociativeCache.HARD);

    def apply(uri: IURIFile): IFileCache =  associativeCache.cache(uri, uri, new FileCacheDeleteOnFinalize(uri.cache))
    
    def apply(uri: IURIFile, token: AccessToken): IFileCache = associativeCache.cache(uri, uri, new FileCacheDeleteOnFinalize(uri.cache(token)));
    
    def invalidate(uri: IURIFile) = associativeCache.invalidateCache(uri, uri)

}
