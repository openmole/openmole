/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.misc.widget.multirow

import java.awt.Color
import scala.collection.mutable.HashSet
import scala.swing.Label
import scala.swing.event.ButtonClicked
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.RowWidget._

object MultiWidget extends Enumeration {

  class Minus(val name: String) extends Val(name)

  val NO_EMPTY = new Minus("NO_EMPTY")
  val CLOSE_IF_EMPTY = new Minus("CLOSE_IF_EMPTY")
}

import MultiWidget._
class MultiWidget[T<:IRowWidget](title: String = "",
                                 rWidgets: List[T],
                                 factory: IRowWidgetFactory[T],
                                 nbComponent: Int,
                                 allowEmpty: Minus= NO_EMPTY){
  val specimen = rWidgets.head
  val rowWidgets = new HashSet[T]
  val panel =  new PluginPanel("wrap "+{if(rWidgets.head.plusAllowed == ADD) 1 else 0}.toString +", insets 0 5 -2 5")
  val titleLabel = new Label(title){foreground = new Color(0,113,187)}
  panel.contents += (titleLabel,"wrap")
  
  rWidgets.foreach {
    r => 
      addRow(factory(r, panel))
  }
  
  def addRow: T = addRow(factory.apply(specimen,panel))
 
  def addRow(rowWidget: T):T = {
    rowWidgets += rowWidget
    panel.contents += rowWidget.panel
    
    panel.listenTo(rowWidget.panel.`removeButton`)
    panel.reactions += {
      case ButtonClicked(rowWidget.panel.`removeButton`) => 
        if (allowEmpty == CLOSE_IF_EMPTY || (allowEmpty == NO_EMPTY && rowWidgets.size > 1)) {
          removeRow(rowWidget)
          rowWidget.doOnClose
        }
    }
    titleLabel.visible = true
    panel.listenTo(rowWidget.panel.`addButton`)
    panel.reactions += {
      case ButtonClicked(rowWidget.panel.`addButton`) => 
        addRow(factory(rowWidget,panel))
    }
    refresh
    rowWidget
  }
  
  def removeRow(rowWidget: T) = {
    rowWidgets -= rowWidget
    panel.contents -= rowWidget.panel
    refresh
  }
  
  def refresh = {
    if (rowWidgets.isEmpty && allowEmpty == CLOSE_IF_EMPTY) titleLabel.visible = false
    panel.repaint
    panel.revalidate
  } 
}
