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
import scala.swing.Component
import scala.swing.Label
import scala.swing.event.ButtonClicked

abstract class MultiWidget[T<:IRowWidget](val rowName: String, rWidgets: List[T], factory: T=>T,nbComponent: Int){
  val rowWidgets = new HashSet[T]
  val panel =  new MigPanel("wrap "+(nbComponent + 3).toString)
  
  rWidgets.foreach(r=>showComponents(addRow(r)))
  
  def addRow(rowWidget: T): List[Component] = {
    rowWidgets+= rowWidget
    val label= new Label(rowName)
    val addButton = buildAddButton(rowWidget)
    val removeList:List[Component] = List(List(label), rowWidget.components,List(addButton)).flatten
    removeList ::: List(buildRemoveButton(removeList,rowWidget))
  }
  
  def buildRemoveButton(lico: List[Component],rowWidget: T) = {
    val rButton = new Button
    rButton.icon = new ImageIcon(ImageTool.loadImage("img/removeRow.png",10,10))
    panel.listenTo(`rButton`)
    panel.reactions += {case ButtonClicked(`rButton`) => if (rowWidgets.size > 1 ) {
          hideComponents(rButton::lico)
          rowWidgets-= rowWidget}}
    rButton}
  
  def buildAddButton(rowWidget: T) = {
    val aButton = new Button
    aButton.icon = new ImageIcon(ImageTool.loadImage("img/addRow.png",10,10))
    panel.listenTo(`aButton`)
    panel.reactions += {case ButtonClicked(`aButton`) => showComponents(addRow(factory(rowWidget)))}
    aButton}
  
  private def showComponents(lico: List[Component]) = {lico.foreach(panel.contents+=)
                                                       refresh
                                                       lico}
  
  private def hideComponents(lico: List[Component]) = {lico.foreach(panel.contents-=);refresh}
  
  private def refresh = {
    panel.repaint
    panel.revalidate
  } 
}
