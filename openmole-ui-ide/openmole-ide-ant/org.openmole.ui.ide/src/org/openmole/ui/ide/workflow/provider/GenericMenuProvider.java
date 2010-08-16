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
package org.openmole.ui.ide.workflow.provider;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.netbeans.api.visual.widget.Widget;
import org.openmole.ui.ide.workflow.model.IGenericMenuProvider;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class GenericMenuProvider implements IGenericMenuProvider {

    Collection<JMenuItem> items = new ArrayList<JMenuItem>();
    Collection<JMenu> menus = new ArrayList<JMenu>();

    @Override
    public Collection<JMenuItem> getItems() {
        return items;
    }

    @Override
    public Collection<JMenu> getMenus() {
        return menus;
    }

    @Override
    public JPopupMenu getPopupMenu(Widget widget, Point point) {
        return PopupMenuProviderFactory.fillPopupMenu(this);
    }
}
