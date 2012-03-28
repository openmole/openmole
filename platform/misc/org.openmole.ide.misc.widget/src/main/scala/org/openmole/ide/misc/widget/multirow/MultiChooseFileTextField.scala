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

import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.FileChooser._
import scala.swing.Panel

object  MultiChooseFileTextField {
  class Factory extends IRowWidgetFactory[ChooseFileTextFieldRowWidget]{
    def apply(row: ChooseFileTextFieldRowWidget, panel: Panel) = {
      import row._
      new ChooseFileTextFieldRowWidget(initValue,chooserTitle,chooserDescription,selectionMode,extensions)
    }
  }
  
  class ChooseFileTextFieldRowWidget(val initValue: String, 
                                     val chooserTitle: String="", 
                                     val chooserDescription: Option[String]=None, 
                                     val selectionMode: SelectionMode.Value = SelectionMode.FilesOnly,
                                     val extensions: Option[String]= None) extends IRowWidget1[String]{
    val fileTextField = new ChooseFileTextField(initValue,chooserTitle,chooserDescription,selectionMode,extensions)
    
    override val panel = new RowPanel(List(fileTextField))
  
    override def content: String = fileTextField.text
  }
}
import MultiChooseFileTextField._
class MultiChooseFileTextField(title: String,
                               initValues: List[String], 
                               chooserTitle: String="", 
                               chooserDescription: Option[String]=None, 
                               selectionMode: SelectionMode.Value= SelectionMode.FilesOnly,
                               extensions: Option[String]= None,
                               factory: IRowWidgetFactory[ChooseFileTextFieldRowWidget],
                               minus: Minus) extends MultiWidget(title,
                                                                 if (initValues.isEmpty) List(new ChooseFileTextFieldRowWidget("",chooserTitle,chooserDescription,selectionMode,extensions)) 
                                                                 else initValues.map(iv=>new ChooseFileTextFieldRowWidget(iv,chooserTitle,chooserDescription,selectionMode,extensions)),
                                                                 factory,1,minus) {  
  def this(title: String,
           iValues: List[String], 
           cTitle: String, 
           cDescription: Option[String], 
           sMode: SelectionMode.Value,
           exts: Option[String]) = this(title,
                                        iValues,
                                        cTitle,
                                        cDescription,
                                        sMode,
                                        exts,
                                        new Factory,
                                        NO_EMPTY)
  def this(title: String,iValues: List[String])= 
    this (title,iValues,"",None,SelectionMode.FilesOnly,None)
  
  def this(title: String,iValues: List[String],selectionMode: SelectionMode.Value)= 
    this (title,iValues,"",None,selectionMode,None)
  
  def content = rowWidgets.map(_.content).toList 
}

