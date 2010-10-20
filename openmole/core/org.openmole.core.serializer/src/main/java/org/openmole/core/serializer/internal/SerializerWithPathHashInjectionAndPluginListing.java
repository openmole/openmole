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

package org.openmole.core.serializer.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import org.openmole.commons.tools.io.IHash;

/**
 *
 * @author reuillon
 */
public class SerializerWithPathHashInjectionAndPluginListing extends SerializerWithPluginClassListing {

    final Map<File, IHash> files = new TreeMap<File, IHash>();

    public SerializerWithPathHashInjectionAndPluginListing() {
        super();
        registerConverter(new FilePathHashNotifier(this));
    }

    IHash fileUsed(File file) throws IOException {
        IHash hash = files.get(file);
        if(hash == null) {
            hash = Activator.getHashService().computeHash(file);
            files.put(file, hash);
        }
        return hash;
    }
    
    @Override
    public void clean() {
        files.clear();
        super.clean();
    }

    public Map<File, IHash> getFiles() {
        return files;
    }
    
}
