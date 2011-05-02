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

import org.openmole.ide.core.workflow.implementation.TaskUI
import org.openmole.ide.core.workflow.implementation.SamplingUI
import org.openmole.ide.core.workflow.implementation.PrototypeUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.properties.IPrototypeFactoryUI
import org.openmole.ide.core.properties.ISamplingFactoryUI
import scala.collection.mutable.HashMap
import org.openide.nodes.Node
import org.openide.nodes.Children
import scala.collection.JavaConversions._
import org.openide.util.Lookup
import java.awt.datatransfer.DataFlavor

class CategoryBuilder extends Children.Keys[ICategory] {
//  var categories= new HashMap[MoleConcepts.Concept, ICategory]
//  categories+= MoleConcepts.TASK_INSTANCE -> new TaskCategory
//  categories+= MoleConcepts.SAMPLING_INSTANCE -> new SamplingCategory
//  categories+= MoleConcepts.PROTOTYPE_INSTANCE -> new PrototypeCategory
  
  override protected def createNodes(key: ICategory) = Array[Node](new CategoryNode(key.asInstanceOf[ICategory]))
  
  override def addNotify = {
    super.addNotify
    // setKeys(categories.values)
    setKeys(List(
        new GenericCategory("Task models", new GenericChildren(PaletteElementFactories.taskModelPaletteElementFactories,new DataFlavor(classOf[TaskUI],"TaskModels"))),
       
        new GenericCategory("Sampling models", new GenericChildren(PaletteElementFactories.samplingModelPaletteElementFactories,new DataFlavor(classOf[SamplingUI],"SamplingModels"))),
       
         //       new GenericCategory("Sampling models", new GenericChildren(JavaConversions.asScalaIterable(Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI])),new DataFlavor(classOf[SamplingUI], "Samplings"))),
        new GenericCategory("Prototype models", new GenericChildren(PaletteElementFactories.prototypeModelPaletteElementFactories,new DataFlavor(classOf[PrototypeUI], "Prototypes")))).toIterable)
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