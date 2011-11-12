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
import org.openmole.ide.misc.widget.ChooseFileTextField
import scala.swing.Component
import scala.swing.FileChooser.SelectionMode._
import scala.swing.Panel

object  MultiChooseFileTextField {
  class Factory extends IRowWidgetFactory[ChooseFileTextFieldRowWidget]{
    def apply(row: ChooseFileTextFieldRowWidget, panel: Panel) = {
      import row._
      new ChooseFileTextFieldRowWidget(name,"",chooserTitle,chooserDescription,selectionMode,extensions)
    }
  }
  
  class ChooseFileTextFieldRowWidget(val name: String,
                                     val initValue: String, 
                                     val chooserTitle: String="", 
                                     val chooserDescription: Option[String]=None, 
                                     val selectionMode: Value= FilesOnly,
                                     val extensions: Option[String]= None) extends IRowWidget1[String]{
    val fileTextField = new ChooseFileTextField(initValue,chooserTitle,chooserDescription,selectionMode,extensions)
    
    override val panel = new RowPanel(name,List(fileTextField))
    
    // var components = List(fileTextField.asInstanceOf[Component])
  
    override def content: String = fileTextField.text
  }
}
import MultiChooseFileTextField._
class MultiChooseFileTextField(rowName: String,
                               initValues: List[String], 
                               chooserTitle: String="", 
                               chooserDescription: Option[String]=None, 
                               selectionMode: Value= FilesOnly,
                               extensions: Option[String]= None,
                               factory: IRowWidgetFactory[ChooseFileTextFieldRowWidget]) extends MultiWidget(if (initValues.isEmpty) List(new ChooseFileTextFieldRowWidget(rowName,"",chooserTitle,chooserDescription,selectionMode,extensions)) 
                                                                                                             else initValues.map(iv=>new ChooseFileTextFieldRowWidget(rowName,iv,chooserTitle,chooserDescription,selectionMode,extensions)),
                                                                                                             factory,1) {  
  def this(rName:String,
           iValues: List[String], 
           cTitle: String, 
           cDescription: Option[String], 
           sMode: Value,
           exts: Option[String]) = this(rName,
                                        iValues,
                                        cTitle,
                                        cDescription,
                                        sMode,
                                        exts,
                                        new Factory)
  def this(rName:String,iValues: List[String])= this (rName,iValues,"",None,FilesOnly,None)
  def content = rowWidgets.map(_.content).toList 
}

