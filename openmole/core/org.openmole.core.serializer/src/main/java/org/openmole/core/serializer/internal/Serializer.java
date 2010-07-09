/*
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.core.serializer.internal;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.core.serializer.ISerializer;


public class Serializer implements ISerializer {

    final XStream xstream = new XStream();
  

    @Override
    public <T> T deserialize(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            return (T) deserialize(is);
        } finally {
            is.close();
        }
    }
    
    @Override
    public <T> T deserialize(InputStream is) {
         return (T) xstream.fromXML(is);
    }

    @Override
    public Iterable<Class> serializeAsHashAndGetPluginClass(Object object, File dir) throws IOException, InternalProcessingError {
        File serialized = Activator.getWorkspace().newFile();
        Iterable<Class> ret = serializeAndGetPluginClass(object, serialized);
        File hashName = new File(dir, Activator.getHashService().computeHash(serialized).toString());
        serialized.renameTo(hashName);
        return ret;
    }
     
    @Override
    public Iterable<Class> serializeAndGetPluginClass(Object object, File file) throws IOException, InternalProcessingError {
        OutputStream zos = new FileOutputStream(file);

        try {
            return serializeAndGetPluginClass(object, zos);
        } finally {
            zos.close();
        }
    }

    @Override
    public Iterable<Class> serializeAndGetPluginClass(Object object, OutputStream os) throws InternalProcessingError {
         try {
            ISerializerWithExtensibleClassListing serializer = SerializerFactory.GetInstance().borrowObject();
            try {
                serializer.toXMLAndListPlugableClasses(object, os);
                return serializer.getExtensibleClasses();
            } finally {
                SerializerFactory.GetInstance().returnObject(serializer);
            }
        } catch (Exception ex) {
            throw new InternalProcessingError(ex);
        } 

    }

    @Override
    public void serialize(Object description, OutputStream file) {
        xstream.toXML(description, file);
    }

    @Override
    public void serialize(Object description, File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        try {
            serialize(description, os);
        } finally {
            os.close();
        }
    }
    
    @Override
    public void serializeAsHash(Object description, File dir) throws IOException, InternalProcessingError {
        File serialized = Activator.getWorkspace().newFile();
        serialize(description, serialized);
        File hashName = new File(dir, Activator.getHashService().computeHash(serialized).toString());
        serialized.renameTo(hashName);
    }
}
