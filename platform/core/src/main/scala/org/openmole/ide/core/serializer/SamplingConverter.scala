/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.serializer

import org.openmole.ide.core.workflow.implementation.EntityUI
import org.openmole.ide.core.workflow.implementation.SamplingUI
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter

class SamplingConverter extends Converter{

  override def marshal(o: Object,writer: HierarchicalStreamWriter,mc: MarshallingContext) = {
 //   val s= o.asInstanceOf[SamplingUI]
    val s= o.asInstanceOf[EntityUI]
    writer.addAttribute("name", s.panelUIData.name)
    writer.addAttribute("type", s.getClass.toString)
  }
  override def unmarshal(reader: HierarchicalStreamReader,uc: UnmarshallingContext) = new Object
 // override def unmarshal(reader: HierarchicalStreamReader,uc: UnmarshallingContext) =  {
   // val f = ElementFactories.
   // new SamplingUI(reader.getAttribute("name"), Class.forName(reader.getAttribute("type")))
 // }
  
  override def canConvert(t: Class[_]) = t.equals(classOf[SamplingUI])
}



//package org.openmole.ide.core.serializer;
//
//import com.thoughtworks.xstream.converters.Converter;
//import com.thoughtworks.xstream.converters.MarshallingContext;
//import com.thoughtworks.xstream.converters.UnmarshallingContext;
//import com.thoughtworks.xstream.io.HierarchicalStreamReader;
//import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
//import org.openmole.ide.core.exception.MoleExceptionManagement;
//import org.openmole.ide.core.workflow.implementation.SamplingUI;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
// */
//public class SamplingConverter implements Converter {
//
//    @Override
//    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
//        SamplingUI s = (SamplingUI) o;
//        writer.addAttribute("name", s.getName());
//        writer.addAttribute("type", s.getType().getName().toString());
//    }
//
//    @Override
//    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
//        SamplingUI sampling = null;
//        try {
//            sampling = new SamplingUI(reader.getAttribute("name"), Class.forName(reader.getAttribute("type")));
//        } catch (ClassNotFoundException ex) {
//            MoleExceptionManagement.showException("Unknown sampling class " + reader.getAttribute("type"));
//        }
//        return sampling;
//    }
//
//    @Override
//    public boolean canConvert(Class type) {
//        return type.equals(SamplingUI.class);
//    }
//}