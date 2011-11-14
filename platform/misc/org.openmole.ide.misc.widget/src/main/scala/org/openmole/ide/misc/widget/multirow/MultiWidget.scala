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

import org.openmole.ide.misc.widget.MigPanel
import scala.collection.mutable.HashSet
import scala.swing.event.ButtonClicked

abstract class MultiWidget[T<:IRowWidget](rWidgets: List[T], factory: IRowWidgetFactory[T],nbComponent: Int){
  val rowWidgets = new HashSet[T]
  val panel =  new MigPanel("wrap "+(nbComponent + 3).toString +", insets -2 5 -2 5")
  
  rWidgets.foreach(r=>showComponents(addRow(factory.apply(r,panel))))
  
  def addRow(rowWidget: T):T = {
    rowWidgets+= rowWidget
    
    panel.listenTo(rowWidget.panel.`removeButton`)
    panel.reactions += {case ButtonClicked(rowWidget.panel.`removeButton`) => if (rowWidgets.size > 1 ) {
          hideComponents(rowWidget)
          rowWidgets-= rowWidget
        }
    }
    
    panel.listenTo(rowWidget.panel.`addButton`)
    panel.reactions += {case ButtonClicked(rowWidget.panel.`addButton`) => showComponents(addRow(factory.apply(rowWidget,panel)))}
    rowWidget
  }
  
  private def showComponents(rowWidget: T) = {panel.contents+=(rowWidget.panel,"wrap");refresh}
  
  private def hideComponents(rowWidget: T) = {panel.contents-=rowWidget.panel;refresh}
  
  def refresh = {
    panel.repaint
    panel.revalidate
  } 
}
