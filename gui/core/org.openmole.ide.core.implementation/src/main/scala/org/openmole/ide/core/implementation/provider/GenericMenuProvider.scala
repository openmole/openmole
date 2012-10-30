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

import java.awt.Point
import javax.swing.JMenu
import javax.swing.JMenuItem
import org.netbeans.api.visual.widget.Widget
import scala.collection.mutable.HashSet
import org.openmole.ide.core.model.provider.IGenericMenuProvider
import scala.collection.mutable.ListBuffer

class GenericMenuProvider extends IGenericMenuProvider {

  var items = ListBuffer.empty[JMenuItem]
  var menus = HashSet.empty[JMenu]
  var currentPoint = new Point(0, 0)

  override def getPopupMenu(widget: Widget, point: Point) = {
    currentPoint = point
    PopupMenuProviderFactory.fillPopupMenu(this)
  }
}