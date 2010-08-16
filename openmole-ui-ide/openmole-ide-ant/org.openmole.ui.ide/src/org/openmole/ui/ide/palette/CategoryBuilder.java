/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
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
package org.openmole.ui.ide.palette;

import java.util.HashMap;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openmole.ui.ide.palette.Category.CategoryName;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class CategoryBuilder extends Children.Keys {

    private static CategoryBuilder instance = null;
    private HashMap<CategoryName, ICategory> categories = new HashMap<CategoryName, ICategory>();

    public CategoryBuilder() {
        categories.put(CategoryName.TASK, new TaskCategory());
        categories.put(CategoryName.TASK_CAPSULE, new TaskCapsuleCategory());
        categories.put(CategoryName.PROTOTYPE, new PrototypeCategory());
        categories.put(CategoryName.PROTOTYPE_INSTANCE, new PrototypeInstanceCategory());
    }

    @Override
    protected Node[] createNodes(Object key) {
        ICategory obj = (ICategory) key;
        return new Node[]{new CategoryNode(obj)};
    }

    @Override
    protected void addNotify() {
        super.addNotify();
        setKeys(categories.values());
    }

    public ICategory getPrototypeInstanceCategory(CategoryName cname) {
        return categories.get(cname);
    }

    public static CategoryBuilder getInstance() {
        if (instance == null) {
            instance = new CategoryBuilder();
        }
        return instance;
    }
}
