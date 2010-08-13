/*
 *  Copyright (C) 2010 reuillon
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
import com.thoughtworks.xstream.XStreamException;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;

/**
 *
 * @author reuillon
 */
public class DeserializerWithFileInjection {

    final private XStream xstream = new XStream();
    private Map<File, File> files = null;

    DeserializerWithFileInjection() {
        xstream.registerConverter(new FileConverterInjecter(this));
    }

    void setFiles(Map<File, File> files) {
        this.files = files;
    }

    <T> T fromXMLInjectFiles(InputStream is) throws InternalProcessingError {
        if (files == null) {
            throw new InternalProcessingError("File map has not been initialized");
        }
        try {
            return (T) xstream.fromXML(is);
        } catch (XStreamException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    void clean() {
        files = null;
    }
    
    File getMatchingFile(File file) {
        return files.get(file);
    }
}
