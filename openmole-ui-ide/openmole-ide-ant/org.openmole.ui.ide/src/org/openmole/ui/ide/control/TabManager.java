/*
 *  Copyright (C) 2010 mathieu
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
package org.openmole.ui.ide.control;

import java.awt.Component;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.JTabbedPane;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

/**
 *
 * @author mathieu
 */
public abstract class TabManager implements ITabManager {

    private BidiMap<Object, Component> tabMap = new DualHashBidiMap<Object, Component>();
   // private Map<Object, Component> tabMap = new WeakHashMap<Object, Component>();
    private JTabbedPane tabbedPane;

    @Override
    public void display(Object displayed) {
        if (!tabMap.containsKey(displayed)) {
            addTab(displayed);
        }
        tabbedPane.setSelectedComponent(tabMap.get(displayed));
    }

    @Override
    public void addMapping(Object obj,
                           Component comp,
                           String name) {
        tabMap.put(obj, comp);
        tabbedPane.add(name, tabMap.get(obj));
    }

    public void setTabbedPane(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    public Object getCurrentObject(){
        return tabMap.getKey(tabbedPane.getSelectedComponent());
    }
}
