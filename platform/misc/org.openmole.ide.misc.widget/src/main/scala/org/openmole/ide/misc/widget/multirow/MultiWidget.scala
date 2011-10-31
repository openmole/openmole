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

import javax.swing.ImageIcon
import org.openmole.ide.misc.image.ImageTool
import org.openmole.ide.misc.widget.MigPanel
import scala.collection.mutable.HashSet
import scala.swing.Button
import scala.swing.Label
import scala.swing.event.ButtonClicked

class MultiWidget(rowName: String, rWidgets: List[IRowWidget], nbComponent: Int) extends MigPanel("wrap "+(nbComponent + 3).toString){
 private val rowWidgets = new HashSet[IRowWidget]
  rWidgets.foreach(addRow)
  
  def content =rowWidgets.map(_.content).toList
  
  private def addRow(rowWidget: IRowWidget): Unit = {
    rowWidgets+= rowWidget
    
    val label= new Label(rowName)
    val addButton = buildAddButton(rowWidget)
    val removeButton = buildRemoveButton(label,rowWidget,addButton)
  
    contents+= label
    rowWidget.components.foreach(contents+=)
    contents+= addButton
    contents+= removeButton
    refresh
  }
  
  private def buildRemoveButton(label: Label, rWidget: IRowWidget, aButton: Button) = {
    val rButton = new Button
    rButton.icon = new ImageIcon(ImageTool.loadImage("img/removeRow.png",10,10))
    listenTo(`rButton`)
    reactions += {case ButtonClicked(`rButton`) =>
        if (rowWidgets.size > 1 ) {
          rWidget.components.foreach(contents-=)
          contents-= label
          contents-= aButton
          contents-= rButton
          rowWidgets-= rWidget
          refresh}}
    rButton}
  
  private def buildAddButton(rowWidget: IRowWidget) = {
    val aButton = new Button
    aButton.icon = new ImageIcon(ImageTool.loadImage("img/addRow.png",10,10))
    listenTo(`aButton`)
    reactions += {case ButtonClicked(`aButton`) => addRow(rowWidget.buildEmptyRow)}
    aButton}
  
  private def refresh = {
    repaint
    revalidate
  } 
}
