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

package org.openmole.core.serializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.openmole.commons.exception.InternalProcessingError;


public interface ISerializer {

    Iterable<Class> serializeAsHashAndGetPluginClass(Object object, File dir) throws IOException, InternalProcessingError;
    Iterable<Class> serializeAndGetPluginClass(Object object, File file) throws IOException, InternalProcessingError;
    Iterable<Class> serializeAndGetPluginClass(Object object, OutputStream file) throws InternalProcessingError;

    <T> T deserialize(File file) throws IOException;
    <T> T deserialize(InputStream file);
    
    void serialize(Object description, OutputStream file);
    void serialize(Object description, File file) throws IOException;
    void serializeAsHash(Object description, File file) throws IOException, InternalProcessingError;
}
