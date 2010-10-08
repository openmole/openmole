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
package org.openmole.core.serializer.internal;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Set;
import java.util.TreeSet;
import org.openmole.commons.exception.InternalProcessingError;

/**
 *
 * @author reuillon
 */
public class SerializerWithFileAndPluginListing extends SerializerWithPluginClassListing {

    private Set<File> files = null;

    SerializerWithFileAndPluginListing() {
        super();
        registerConverter(new FileConverterNotifier(this));
    }

    void fileUsed(File file) {
        files.add(file);
    }

    @Override
    void toXMLAndListPlugableClasses(Object obj, OutputStream outputStream) throws InternalProcessingError {
        files = new TreeSet<File>();
        super.toXMLAndListPlugableClasses(obj, outputStream);
    }

    Set<File> getFiles() {
        return files;
    }

    @Override
    void clean() {
        super.clean();
        files = null;
    }
}
