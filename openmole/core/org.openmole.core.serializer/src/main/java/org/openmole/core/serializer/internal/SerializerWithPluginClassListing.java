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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import org.openmole.commons.exception.InternalProcessingError;

/**
 *
 * @author reuillon
 */
public class SerializerWithPluginClassListing {

    final private XStream xstream = new XStream();
    private Set<Class> classes = null;

    SerializerWithPluginClassListing() {
        registerConverter(new PluginConverter(this, new ReflectionConverter(xstream.getMapper(), xstream.getReflectionProvider())));
    }

    protected void registerConverter(Converter converter) {
        xstream.registerConverter(converter);
    }
    
    protected void registerConverter(SingleValueConverter converter) {
        xstream.registerConverter(converter);
    }
    
    void classUsed(Class c) {
        classes.add(c);
    }

    void toXMLAndListPlugableClasses(Object obj, OutputStream outputStream) throws InternalProcessingError {
        classes = new HashSet<Class>();
        xstream.toXML(obj, outputStream);
    }

    Set<Class> getClasses() {
        return classes;
    }

    void clean() {
        classes = null;
    }
}
