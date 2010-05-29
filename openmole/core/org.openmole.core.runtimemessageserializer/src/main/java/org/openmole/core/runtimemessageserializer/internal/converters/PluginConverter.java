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

package org.openmole.core.runtimemessageserializer.internal.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.openmole.core.runtimemessageserializer.internal.Activator;
import org.openmole.core.runtimemessageserializer.internal.ISerializerWithExtensibleClassListing;

/**
 *
 * @author reuillon
 */
public class PluginConverter implements Converter {

    private final ISerializerWithExtensibleClassListing serializer;
    private final ReflectionConverter reflectionConverter;

    public PluginConverter(ISerializerWithExtensibleClassListing serializer, ReflectionConverter reflectionConverter) {
        this.serializer = serializer;
        this.reflectionConverter = reflectionConverter;
    }

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {       
        serializer.classUsed(o.getClass());
        reflectionConverter.marshal(o, writer, mc);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
         return reflectionConverter.unmarshal(reader, uc);
    }

    @Override
    public boolean canConvert(Class type) {
       return Activator.getPluginManager().isClassProvidedByAPlugin(type);
    }

}
