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

import scala.collection.mutable.HashMap
import org.openide.nodes.Node
import org.openide.nodes.Children
import scala.collection.JavaConversions._

class CategoryBuilder extends Children.Keys[ICategory] {
  var categories= new HashMap[MoleConcepts.Concept, ICategory]
  categories+= MoleConcepts.TASK_INSTANCE -> new TaskCategory
  categories+= MoleConcepts.SAMPLING_INSTANCE -> new SamplingCategory
  categories+= MoleConcepts.PROTOTYPE_INSTANCE -> new PrototypeCategory
  
  override protected def createNodes(key: ICategory) = Array[Node](new CategoryNode(key.asInstanceOf[ICategory]))
  
  override def addNotify = {
    super.addNotify
    setKeys(categories.values)
  }
  
  def prototypeInstanceCategory(cname: MoleConcepts.Concept)= categories(cname)
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