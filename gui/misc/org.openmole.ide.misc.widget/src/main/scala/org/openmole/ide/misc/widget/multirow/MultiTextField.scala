/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.TextField

object MultiTextField {

  class TextFieldPanel(val data: TextFieldData) extends PluginPanel("wrap 2") with IPanel[TextFieldData] {

    val textField = new TextField(data.textFieldValue, 15)

    contents += textField

    def content = new TextFieldData(textField.text)
  }

  class TextFieldData(val textFieldValue: String = "") extends IData

  class TextFieldFactory extends IFactory[TextFieldData] {
    def apply = new TextFieldPanel(new TextFieldData)
  }
}

import MultiTextField._
class MultiTextField(title: String,
                     initPanels: List[TextFieldPanel],
                     minus: Minus = NO_EMPTY,
                     plus: Plus = ADD) extends MultiPanel(title,
  new TextFieldFactory,
  initPanels,
  minus,
  plus)
