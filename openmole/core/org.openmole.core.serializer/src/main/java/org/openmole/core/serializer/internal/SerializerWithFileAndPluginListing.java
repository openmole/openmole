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
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import java.io.File;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.openmole.commons.exception.InternalProcessingError;

/**
 *
 * @author reuillon
 */
public class SerializerWithFileAndPluginListing {

  final private XStream xstream = new XStream();   

  private Set<Class> classes = null; //new HashSet<Class>();
  private Set<File> files = null; //new TreeSet<File>();
  
  SerializerWithFileAndPluginListing() {
    xstream.registerConverter(new PluginConverter(this, new ReflectionConverter(xstream.getMapper(), xstream.getReflectionProvider())));
    xstream.registerConverter(new FileConverterNotifier(this));
  }
                              
  void classUsed(Class c) {
    classes.add(c);
  }
  
  void fileUsed(File file) {
    files.add(file);
  }
  
  void toXMLAndListPlugableClasses(Object obj, OutputStream outputStream) throws InternalProcessingError {
      classes = new HashSet<Class>();
      files = new TreeSet<File>();
      try {
          xstream.toXML(obj, outputStream);
      } catch(XStreamException ex) {
          throw new InternalProcessingError(ex);
      }
  }

  Set<File> getFiles() {
    return files;
  }
  
  Set<Class> getClasses() {
    return classes;
  }
  
  void clean() {
    classes = null;
    files = null;
  }
}
