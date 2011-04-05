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

package org.openmole.ui.ide.serializer
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import org.openmole.ui.ide.workflow.implementation.TaskUI

class TaskConverter extends Converter{

  override def marshal(o: Object,writer: HierarchicalStreamWriter,mc: MarshallingContext) = {
    val t= o.asInstanceOf[TaskUI]
    writer.addAttribute("name", t.name)
    writer.addAttribute("type", t.entityType.getName.toString)
  }
  
  override def unmarshal(reader: HierarchicalStreamReader,uc: UnmarshallingContext) =  new TaskUI(reader.getAttribute("name"), Class.forName(reader.getAttribute("type")))
  
  override def canConvert(t: Class[_]) = t.equals(classOf[TaskUI])
}
//package org.openmole.ui.ide.serializer;
//
//import com.thoughtworks.xstream.converters.Converter;
//import com.thoughtworks.xstream.converters.MarshallingContext;
//import com.thoughtworks.xstream.converters.UnmarshallingContext;
//import com.thoughtworks.xstream.io.HierarchicalStreamReader;
//import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
//import org.openmole.ui.ide.exception.MoleExceptionManagement;
//import org.openmole.ui.ide.workflow.implementation.TaskUI;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
// */
//public class TaskConverter implements Converter{
//
//    @Override
//    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
//        TaskUI t = (TaskUI) o;
//        writer.addAttribute("name", t.getName());
//        writer.addAttribute("type", t.getType().getName().toString());
//    }
//
//    @Override
//    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
//        TaskUI task = null;
//        try {
//            task = new TaskUI(reader.getAttribute("name"), Class.forName(reader.getAttribute("type")));
//        } catch (ClassNotFoundException ex) {
//            MoleExceptionManagement.showException("Unknown task class " + reader.getAttribute("type"));
//        }
//        return task;
//    }
//
//    @Override
//    public boolean canConvert(Class type) {
//        return type.equals(TaskUI.class);
//    }
//
//}