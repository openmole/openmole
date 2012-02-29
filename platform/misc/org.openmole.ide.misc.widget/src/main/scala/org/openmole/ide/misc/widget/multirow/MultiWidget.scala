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
import scala.swing.Action
import scala.swing.Label
import javax.swing.ImageIcon
import org.openmole.ide.misc.image.ImageTool
import org.openmole.ide.misc.widget.MainLinkLabel
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
  val panel =  new PluginPanel("wrap "+{if(rWidgets.head.plusAllowed == ADD) 1 else 0}.toString +", insets 0 5 0 5")
  val titleLabel = new Label(title){foreground = new Color(0,113,187)}
  val addButton = new MainLinkLabel("",new Action("") { def apply = addRow }) {
    icon = new ImageIcon(ImageTool.loadImage("img/add.png",15,15))}
  
  rWidgets.foreach {
    r => 
    addRow(factory(r, panel))
  }
  panel.contents.insert(0,titleLabel)
  panel.contents += addButton
    
  def addRow: T = addRow(factory.apply(specimen,panel))
 
  def addRow(rowWidget: T):T = {
    rowWidgets += rowWidget
    panel.contents.insert(panel.contents.size-1,rowWidget.panel)
    
    rowWidget.panel.removeButton.action_=( new Action("") {def apply = {
          if (allowEmpty == CLOSE_IF_EMPTY || (allowEmpty == NO_EMPTY && rowWidgets.size > 1)) {
            removeRow(rowWidget)
            rowWidget.doOnClose}}})
    refresh
    rowWidget
  }
  
  def removeAllRows = rowWidgets.foreach(removeRow)
  
  def removeRow(rowWidget: T) = {
    rowWidgets -= rowWidget
    panel.contents -= rowWidget.panel
    refresh
  }
  
  def refresh = {
    panel.repaint
    panel.revalidate
  } 
}
