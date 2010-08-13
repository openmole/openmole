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

import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.extended.FileConverter;
import java.io.File;

/**
 *
 * @author reuillon
 */
public class FileConverterInjecter extends FileConverter {

    final DeserializerWithFileInjection deserializer;

    public FileConverterInjecter(DeserializerWithFileInjection deserializer) {
        this.deserializer = deserializer;
    }

    @Override
    public Object fromString(String str) {
        File file = (File) super.fromString(str);
        File ret = deserializer.getMatchingFile(file);
        if(ret == null) throw new XStreamException("No matching file for " + file.getAbsolutePath());
        return ret;
    }
}
