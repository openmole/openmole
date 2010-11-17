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

package org.openmole.core.serializer.internal



import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.SingleValueConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import java.io.OutputStream
import scala.collection.mutable.HashSet

class SerializerWithPluginClassListing {

    private val xstream = new XStream
    var classes: HashSet[Class[_]] = null

    registerConverter(new PluginConverter(this, new ReflectionConverter(xstream.getMapper(), xstream.getReflectionProvider())));
 
    protected def registerConverter(converter: Converter) = {
        xstream.registerConverter(converter)
    }
    
    protected def registerConverter(converter: SingleValueConverter) =  {
        xstream.registerConverter(converter)
    }
    
    def classUsed(c: Class[_]) {
        classes.add(c)
    }

    def toXMLAndListPlugableClasses(obj: Object, outputStream: OutputStream) = {
        classes = new HashSet[Class[_]]
        xstream.toXML(obj, outputStream);
    }

    def clean(): Unit = {
        classes = null
    }
}
