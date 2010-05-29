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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import org.openmole.core.runtimemessageserializer.internal.converters.PluginConverter;
import org.openmole.core.model.message.IJobForRuntime;

/**
 *
 * @author reuillon
 */
public class SerializerWithExtensibleClassListing implements ISerializerWithExtensibleClassListing{

    XStream xstream = new XStream();
    
    Set<Class> extensibleClasses = new HashSet<Class>();
   // Set<Class> seen = new HashSet<Class>();

    public SerializerWithExtensibleClassListing() {
        xstream = new XStream();
        xstream.registerConverter(new PluginConverter(this, new ReflectionConverter(xstream.getMapper(), xstream.getReflectionProvider())));
    }

    @Override
    public void classUsed(Class c) {
      //  if(seen.contains(c)) return;
      //  else seen.add(c);

        //if(Activator.getPluginManager().isClassProvidedByAPlugin(c)) {
            //Logger.getLogger(SerializerWithExtensibleClassListing.class.getName()).info("Class from pluggin: " + c.getName());
            extensibleClasses.add(c);
        //}
    }

    @Override
    public Iterable<Class> getExtensibleClasses() {
        return extensibleClasses;
    }

    @Override
    public void toXMLAndListPlugableClasses(IJobForRuntime jobForRuntime, OutputStream outputStream) {
        xstream.toXML(jobForRuntime, outputStream);
    }

    @Override
    public void clean() {
        extensibleClasses = new HashSet<Class>();
     //   seen = new HashSet<Class>();
    }

}
