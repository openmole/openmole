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
package org.openmole.core.runtimemessageserializer.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.openmole.commons.exception.InternalProcessingError;

import com.thoughtworks.xstream.XStream;
import org.openmole.core.model.execution.batch.IBatchEnvironmentDescription;
import org.openmole.core.runtimemessageserializer.IBatchEnvironmentDescriptionSerializer;

public class BatchEnvironmentDescriptionSerializer implements IBatchEnvironmentDescriptionSerializer {

    XStream xstream = new XStream();

    @Override
    public void serialize(IBatchEnvironmentDescription description, File file) throws InternalProcessingError {
        try {
            xstream.toXML(description, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new InternalProcessingError(e);
        }
    }

    @Override
    public void serialize(IBatchEnvironmentDescription description, OutputStream file) {
        xstream.toXML(description, file);
    }

    @Override
    public IBatchEnvironmentDescription deserialize(File file)
            throws InternalProcessingError {
        try {
            return (IBatchEnvironmentDescription) xstream.fromXML(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new InternalProcessingError(e);
        }
    }
}
