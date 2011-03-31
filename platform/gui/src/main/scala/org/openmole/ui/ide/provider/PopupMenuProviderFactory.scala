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

package org.openmole.ui.ide.provider

import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

object PopupMenuProviderFactory {

  def fillPopupMenu(gmp: IGenericMenuProvider)= {
    val popupMenu = new JPopupMenu
    gmp.menus.foreach(popupMenu.add(_))
    gmp.items.foreach(popupMenu.add(_))
    popupMenu
  }
  
  def addSubMenu(subMenuTitle: String, its: Set[JMenuItem])= {
    val subMenu = new JMenu(subMenuTitle)
    its.foreach(subMenu.add(_))
    subMenu
  }
}


//public class PopupMenuProviderFactory {
//
//    private static PopupMenuProviderFactory instance = null;
//   
//    public static JPopupMenu fillPopupMenu(IGenericMenuProvider gmp) {
//        JPopupMenu popupMenu = new JPopupMenu();
//
//        for(JMenu menu:gmp.getMenus()){
//            System.out.println("menu :" + menu);
//            popupMenu.add(menu);
//        }
//
//        for(JMenuItem menu:gmp.getItems()){
//            popupMenu.add(menu);
//        }
//        
//        return popupMenu;
//    }
//
//    public static JMenu addSubMenu(String subMenuTitle,
//                                   Collection<JMenuItem> items) {
//
//        JMenu subMenu = new JMenu(subMenuTitle);
//        Iterator<JMenuItem> it = items.iterator();
//        while (it.hasNext()) {
//            subMenu.add(it.next());
//        }
//        return subMenu;
//    }
//
//    public static PopupMenuProviderFactory getInstance() {
//        if (instance == null) {
//            instance = new PopupMenuProviderFactory();
//        }
//        return instance;
//    }