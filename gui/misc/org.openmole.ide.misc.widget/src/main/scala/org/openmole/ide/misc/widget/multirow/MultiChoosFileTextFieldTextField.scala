/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.misc.widget.multirow

import scala.swing.TextField
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.FileChooser._

object MultiChooseFileTextFieldTextField {

  class ChooseFileTextFieldTextFieldPanel(data: ChooseFileTextFieldTextFieldData,
                                          chooserTitle: String = "",
                                          chooserDescription: Option[String] = None,
                                          selectionMode: SelectionMode.Value = SelectionMode.FilesOnly,
                                          extensions: Option[String] = None)
      extends PluginPanel("wrap 2") with IPanel[ChooseFileTextFieldTextFieldData] {

    val chooseFileTextField = new ChooseFileTextField(data.chooseFileContent, chooserTitle, chooserDescription, selectionMode, extensions)
    val textField = new TextField(data.textFieldContent, 15)

    contents += chooseFileTextField
    contents += textField

    def content = new ChooseFileTextFieldTextFieldData(chooseFileTextField.text, textField.text)
  }

  class ChooseFileTextFieldTextFieldData(val chooseFileContent: String = "",
                                         val textFieldContent: String = "") extends IData

  class ChooseFileTextFieldTextFieldFactory(chooserTitle: String = "",
                                            chooserDescription: Option[String] = None,
                                            selectionMode: SelectionMode.Value = SelectionMode.FilesOnly,
                                            extensions: Option[String] = None) extends IFactory[ChooseFileTextFieldTextFieldData] {

    def apply = new ChooseFileTextFieldTextFieldPanel(new ChooseFileTextFieldTextFieldData,
      chooserTitle,
      chooserDescription,
      selectionMode,
      extensions)
  }
}

import MultiChooseFileTextFieldTextField._
class MultiChooseFileTextFieldTextField(title: String,
                                        initPanels: List[ChooseFileTextFieldTextFieldPanel],
                                        chooserTitle: String = "",
                                        chooserDescription: Option[String] = None,
                                        selectionMode: SelectionMode.Value = SelectionMode.FilesOnly,
                                        extensions: Option[String] = None,
                                        minus: Minus = NO_EMPTY,
                                        plus: Plus = ADD) extends MultiPanel(title,
  new ChooseFileTextFieldTextFieldFactory(chooserTitle,
    chooserDescription,
    selectionMode,
    extensions),
  initPanels,
  minus,
  plus)