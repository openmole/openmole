/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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

import java.util.Collection;
import java.util.Iterator;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.openmole.ui.ide.workflow.model.IGenericMenuProvider;
import org.openmole.ui.ide.workflow.model.IObjectModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class PopupMenuProviderFactory {

    private static PopupMenuProviderFactory instance = null;
   
    public static JPopupMenu fillPopupMenu(IGenericMenuProvider gmp) {
        JPopupMenu popupMenu = new JPopupMenu();

        for(JMenu menu:gmp.getMenus()){
            popupMenu.add(menu);
        }

        for(JMenuItem menu:gmp.getItems()){
            popupMenu.add(menu);
        }
        
        return popupMenu;
    }

  /*  public static GenericMenuProvider merge(IGenericMenuProvider gmp1,
                                            IGenericMenuProvider gmp2,
                                            IObjectModelUI mergedModel) {

        //GenericMenuProvider gmp = new GenericMenuProvider(mergedModel);
        GenericMenuProvider gmp = new GenericMenuProvider();
        Iterator<JMenu> itMenu1 = gmp1.getMenus().iterator();
        while (itMenu1.hasNext()) {
            gmp.getMenus().add(itMenu1.next());
        }

        Iterator<JMenu> itMenu2 = gmp2.getMenus().iterator();
        while (itMenu2.hasNext()) {
            gmp.getMenus().add(itMenu2.next());
        }

        Iterator<JMenuItem> it1 = gmp1.getItems().iterator();
        while (it1.hasNext()) {
            gmp.getItems().add(it1.next());
        }
        
        Iterator<JMenuItem> it2 = gmp2.getItems().iterator();
        while (it2.hasNext()) {
            gmp.getItems().add(it2.next());
        }
        return gmp;
    }*/

    public static JMenu addSubMenu(String subMenuTitle,
                                   Collection<JMenuItem> items) {

        JMenu subMenu = new JMenu(subMenuTitle);
        Iterator<JMenuItem> it = items.iterator();
        while (it.hasNext()) {
            subMenu.add(it.next());
        }
        return subMenu;
    }

    public static PopupMenuProviderFactory getInstance() {
        if (instance == null) {
            instance = new PopupMenuProviderFactory();
        }
        return instance;
    }
}
