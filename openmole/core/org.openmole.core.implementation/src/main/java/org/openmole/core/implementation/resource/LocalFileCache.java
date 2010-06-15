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
import java.util.Map;
import java.util.TreeMap;

import org.openmole.core.model.resource.ILocalFileCache;

public class LocalFileCache implements ILocalFileCache {

    Map<File, File> localFileCache;

    @Override
    public File getLocalFileCache(File src) {
        return getLocalFileCache().get(src);
    }

    @Override
    public boolean containsCacheFor(File src) {
        return localFileCache.containsKey(src);
    }

    private Map<File, File> getLocalFileCache() {
        if (localFileCache != null) {
            return localFileCache;
        }

        synchronized (this) {
            if (localFileCache == null) {
                localFileCache = new TreeMap<File, File>();
            }
            return localFileCache;
        }
    }

    public void fillInLocalFileCache(File src, File file) {

        synchronized (getLocalFileCache()) {
            getLocalFileCache().put(src, file);
        }
    }
}
