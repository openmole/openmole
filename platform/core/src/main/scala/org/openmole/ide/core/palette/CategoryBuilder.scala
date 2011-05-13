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

package org.openmole.ide.core.palette

import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.openmole.ide.core.workflow.implementation.SamplingUI
import org.openmole.ide.core.workflow.implementation.PrototypeUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.properties.IPrototypeFactoryUI
import org.openmole.ide.core.properties.ISamplingFactoryUI
import scala.collection.mutable.HashMap
import org.openide.nodes.Node
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import org.openide.nodes.Children
import scala.collection.JavaConversions._
import org.openide.util.Lookup
import java.awt.datatransfer.DataFlavor

class CategoryBuilder extends Children.Keys[ICategory]{
  
  override protected def createNodes(key: ICategory) = Array[Node](new CategoryNode(key.asInstanceOf[ICategory]))
  
  override def addNotify = {
    super.addNotify
    // setKeys(categories.values)
    
    setKeys(List(
        new GenericCategory(Constants.TASK,"Tasks" ,new GenericChildren(ElementFactories.paletteElements(Constants.TASK),Constants.TASK_DATA_FLAVOR)),
        new GenericCategory(Constants.PROTOTYPE,"Prototypes" ,new GenericChildren(ElementFactories.paletteElements(Constants.PROTOTYPE),Constants.PROTOTYPE_DATA_FLAVOR)),
        new GenericCategory(Constants.SAMPLING,"Samplings" ,new GenericChildren(ElementFactories.paletteElements(Constants.SAMPLING),Constants.SAMPLING_DATA_FLAVOR)),
        new GenericCategory(Constants.ENVIRONMENT,"Environments" ,new GenericChildren(ElementFactories.paletteElements(Constants.ENVIRONMENT),Constants.ENVIRONMENT_DATA_FLAVOR))).toIterable)
  }
  

}

//package org.openmole.ide.core.palette;
//
//import java.util.HashMap;
//import org.openide.nodes.Children;
//import org.openide.nodes.Node;
//import org.openmole.ide.core.palette.Category.CategoryName;
//
//public class CategoryBuilder extends Children.Keys {
//
//    private static CategoryBuilder instance = null;
//    private HashMap<CategoryName, ICategory> categories = new HashMap<CategoryName, ICategory>();
//
//    public CategoryBuilder() {
//        categories.put(CategoryName.TASK_INSTANCE, new TaskCategory());
//        categories.put(CategoryName.PROTOTYPE_INSTANCE, new PrototypeCategory());
//        categories.put(CategoryName.SAMPLING_INSTANCE, new SamplingCategory());
//    }
//
//    @Override
//    protected Node[] createNodes(Object key) {
//        ICategory obj = (ICategory) key;
//        return new Node[]{new CategoryNode(obj)};
//    }
//
//    @Override
//    protected void addNotify() {
//        super.addNotify();
//        setKeys(categories.values());
//    }
//
//    public ICategory getPrototypeInstanceCategory(CategoryName cname) {
//        return categories.get(cname);
//    }
//
//    public static CategoryBuilder getInstance() {
//        if (instance == null) {
//            instance = new CategoryBuilder();
//        }
//        return instance;
//    }
//}