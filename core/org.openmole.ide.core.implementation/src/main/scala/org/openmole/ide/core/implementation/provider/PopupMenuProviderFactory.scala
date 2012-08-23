/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.provider

import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import org.openmole.ide.core.model.provider.IGenericMenuProvider

object PopupMenuProviderFactory {

  def fillPopupMenu(gmp: IGenericMenuProvider) = {
    val popupMenu = new JPopupMenu
    gmp.menus.foreach(popupMenu.add(_))
    gmp.items.foreach(popupMenu.add(_))
    popupMenu
  }

  def addSubMenu(subMenuTitle: String, its: Set[JMenuItem]) = {
    val subMenu = new JMenu(subMenuTitle)
    its.foreach(subMenu.add(_))
    subMenu
  }
}