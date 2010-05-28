/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
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

package org.openmole.ui.palette;

import java.awt.datatransfer.DataFlavor;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.lookup.Lookups;
import org.openmole.ui.workflow.implementation.Preferences;
import org.openmole.ui.workflow.implementation.PropertyManager;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class GenericNode extends AbstractNode {

    protected  DataFlavor dataFlavor;

    public GenericNode(DataFlavor key,
                       String type,
                       Class coreClass) {
        super(Children.LEAF, Lookups.fixed(new Object[]{key}));
        this.dataFlavor = key;

        System.out.println("-----------------------------------------------");
        System.out.println("type "+type);
        System.out.println("coreC "+coreClass);
        System.out.println("Preferences.getInstance().getProperties(type,coreClass) " + Preferences.getInstance().getProperties(type,coreClass));
        setIconBaseWithExtension(Preferences.getInstance().getProperties(type,coreClass).getProperty(PropertyManager.THUMB_IMG));
        setName(Preferences.getInstance().getProperties(type, coreClass).getProperty(PropertyManager.NAME));
    }
}
