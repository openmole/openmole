/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.FileChooser._

object MultiChooseFileTextField {

  class ChooseFileTextFieldPanel(data: ChooseFileTextFieldData,
                                 chooserTitle: String = "",
                                 selectionMode: SelectionMode.Value = SelectionMode.FilesOnly,
                                 chooserDescription: Option[(String, Seq[String])] = None)
      extends PluginPanel("wrap 2") with IPanel[ChooseFileTextFieldData] {

    val chooseFileTextField = new ChooseFileTextField(data.content, chooserTitle, selectionMode, chooserDescription)

    contents += chooseFileTextField

    def content = new ChooseFileTextFieldData(chooseFileTextField.text)
  }

  class ChooseFileTextFieldData(val content: String = "") extends IData

  class ChooseFileTextFieldFactory(chooserTitle: String = "",
                                   selectionMode: SelectionMode.Value = SelectionMode.FilesOnly,
                                   chooserDescription: Option[(String, Seq[String])] = None) extends IFactory[ChooseFileTextFieldData] {

    def apply = new ChooseFileTextFieldPanel(new ChooseFileTextFieldData,
      chooserTitle,
      selectionMode,
      chooserDescription)
  }
}

import MultiChooseFileTextField._
class MultiChooseFileTextField(title: String,
                               initPanels: List[ChooseFileTextFieldPanel],
                               chooserTitle: String = "",
                               selectionMode: SelectionMode.Value = SelectionMode.FilesOnly,
                               chooserDescription: Option[(String, Seq[String])] = None,
                               minus: Minus = NO_EMPTY,
                               plus: Plus = ADD) extends MultiPanel(title,
  new ChooseFileTextFieldFactory(chooserTitle,
    selectionMode,
    chooserDescription),
  initPanels,
  minus,
  plus)