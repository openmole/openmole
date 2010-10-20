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

import scala.Tuple2;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.io.File;
import org.openmole.commons.tools.io.FileInputStream;
import org.openmole.commons.tools.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.function.IPartialFunction;
import org.openmole.commons.tools.io.IHash;
import org.openmole.core.serializer.ISerializer;
import static org.openmole.commons.tools.io.FileUtil.*;

/**
 *
 * @author reuillon
 */
public class Serializer implements ISerializer {

    final XStream xstream = new XStream();

    @Override
    public <T> T deserialize(File file) throws InternalProcessingError {
        try {
            InputStream is = new FileInputStream(file);
            try {
                return (T) this.<T>deserialize(is);
            } finally {
                is.close();
            }
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public <T> T deserialize(InputStream is) throws InternalProcessingError {
        try {
            return (T) xstream.fromXML(is);
        } catch (XStreamException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public Tuple2<Map<File, IHash>, Collection<Class>> serializeFilePathAsHashGetPluginClassAndFiles(Object obj, File dir) throws InternalProcessingError, UserBadDataError {
        try {
            SerializerWithPathHashInjectionAndPluginListing serializer = SerializerWithPathHashInjectionAndPluginListingFactory.instance.borrowObject();
            try {
                File serialized = Activator.getWorkspace().newFile();

                OutputStream os = new FileOutputStream(serialized);
                try {
                    serializer.toXMLAndListPlugableClasses(obj, os);
                } finally {
                    os.close();
                }

                File hashName = new File(dir, Activator.getHashService().computeHash(serialized).toString());
                move(serialized, hashName);

                return new Tuple2<Map<File, IHash>, Collection<Class>>(serializer.getFiles(), serializer.getClasses());
            } finally {
                SerializerWithPathHashInjectionAndPluginListingFactory.instance.returnObject(serializer);
            }
        } catch (Exception ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public Tuple2<Collection<File>, Collection<Class>> serializeGetPluginClassAndFiles(Object obj, File file) throws InternalProcessingError {
        try {
            OutputStream os = new FileOutputStream(file);
            try {
                return serializeGetPluginClassAndFiles(obj, os);
            } finally {
                os.close();
            }
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public Tuple2<Collection<File>, Collection<Class>> serializeGetPluginClassAndFiles(Object obj, OutputStream os) throws InternalProcessingError {
        try {
            SerializerWithFileAndPluginListing serializer = SerializerWithFileAndPluginListingFactory.instance.borrowObject();
            try {
                serializer.toXMLAndListPlugableClasses(obj, os);
                return new Tuple2<Collection<File>, Collection<Class>>(serializer.getFiles(), serializer.getClasses());
            } finally {
                SerializerWithFileAndPluginListingFactory.instance.returnObject(serializer);
            }
        } catch (Exception ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public <T> T deserializeReplaceFiles(File file, IPartialFunction<File, File> files) throws InternalProcessingError, InternalProcessingError, InternalProcessingError {
        try {
            InputStream is = new FileInputStream(file);
            try {
                return this.<T>deserializeReplaceFiles(is, files);
            } finally {
                is.close();
            }
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }

    }

    @Override
    public <T> T deserializeReplaceFiles(InputStream is, IPartialFunction<File, File> files) throws InternalProcessingError, InternalProcessingError {
        try {
            DeserializerWithFileInjectionFromFile serializer = DeserializerWithFileInjectionFromFileFactory.instance.borrowObject();
            try {

                serializer.setFiles(files);
                return serializer.<T>fromXMLInjectFiles(is);
            } finally {
                DeserializerWithFileInjectionFromFileFactory.instance.returnObject(serializer);
            }
        } catch (Exception ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public void serialize(Object obj, OutputStream os) throws InternalProcessingError {
        try {
            xstream.toXML(obj, os);
        } catch (XStreamException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public void serialize(Object obj, File file) throws InternalProcessingError {
        try {
            OutputStream os = new FileOutputStream(file);
            try {
                serialize(obj, os);
            } finally {
                os.close();
            }
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public void serializeAsHash(Object obj, File dir) throws InternalProcessingError, UserBadDataError {
        try {
            File serialized = Activator.getWorkspace().newFile();
            serialize(obj, serialized);
            File hashName = new File(dir, Activator.getHashService().computeHash(serialized).toString());
            move(serialized, hashName);
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public <T> T deserializeReplacePathHash(File file, IPartialFunction<IHash, File> files) throws InternalProcessingError {
        try {
            InputStream is = new FileInputStream(file);
            try {
                return this.<T>deserializeReplacePathHash(is, files);
            } finally {
                is.close();
            }
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    public <T> T deserializeReplacePathHash(InputStream it, IPartialFunction<IHash, File> files) throws InternalProcessingError {
        try {
            DeserializerWithFileInjectionFromPathHash deserializer = DeserializerWithFileInjectionFromPathHashFactory.instance.borrowObject();
            try {
                deserializer.setFiles(files);
                return deserializer.<T>fromXMLInjectFiles(it);
            } finally {
                DeserializerWithFileInjectionFromPathHashFactory.instance.returnObject(deserializer);
            }
        } catch (Exception ex) {
            throw new InternalProcessingError(ex);
        }
    }
}
