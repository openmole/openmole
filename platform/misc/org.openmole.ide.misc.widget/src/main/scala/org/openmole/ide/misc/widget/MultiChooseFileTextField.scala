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

package org.openmole.ide.misc.widget

import scala.swing.Label
import scala.swing.Button
import scala.swing.event.ButtonClicked
import javax.swing.ImageIcon
import scala.collection.mutable.HashSet
import org.openmole.ide.misc.image.ImageTool

class MultiChooseFileTextField(labelName: String,initValues: List[String]) extends MigPanel("wrap 4"){
  val textFields = new HashSet[ChooseFileTextField]
  if (initValues.isEmpty) addRow("") else initValues.foreach(addRow) 
  
  def addRow(fieldText: String){
    val label= new Label(labelName)
    val fileTextField= new ChooseFileTextField(fieldText)
    textFields+= fileTextField
    
    val addButton = new Button
    addButton.icon = new ImageIcon(ImageTool.loadImage("img/addRow.png",10,10))
                                   
    
    listenTo(`addButton`)
    reactions += {case ButtonClicked(`addButton`) => addRow("")}
    
    val removeButton = new Button
    removeButton.icon = new ImageIcon(ImageTool.loadImage("img/removeRow.png",10,10))
    listenTo(`removeButton`)
    reactions += {case ButtonClicked(`removeButton`) =>
        if (textFields.size > 1 ) {
          contents-= label
          contents-= fileTextField
          contents-= addButton
          contents-= removeButton
          textFields-= fileTextField
          refresh}}
  
    contents+= label
    contents+= fileTextField
    contents+= addButton
    contents+= removeButton
    refresh
  }
  
  private def refresh = {
    repaint
    revalidate
  } 
  
  def content= textFields.map(_.text).filterNot(t=>t.isEmpty).toList
}
