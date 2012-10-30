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
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.MyComboBox
import scala.swing.TextField

object MultiTextFieldCombo {

  class TextFieldComboPanel[B](val comboContent: List[B],
                               val data: TextFieldComboData[B]) extends PluginPanel("wrap 2") with IPanel[TextFieldComboData[B]] {

    val textField = new TextField(data.textFieldValue, 15)
    val comboBox = new MyComboBox(comboContent.sortBy { _.toString }) {
      data.comboValue match {
        case Some(x: B) ⇒ selection.item = x
        case _ ⇒
      }
    }

    contents += textField
    contents += comboBox

    def content = new TextFieldComboData(textField.text, Some(comboBox.selection.item))
  }

  class TextFieldComboData[B](val textFieldValue: String = "",
                              val comboValue: Option[B] = None) extends IData

  class TextFieldComboFactory[B](comboContent: List[B]) extends IFactory[TextFieldComboData[B]] {
    def apply = new TextFieldComboPanel(comboContent, new TextFieldComboData)
  }
}

import MultiTextFieldCombo._
class MultiTextFieldCombo[B](title: String,
                             comboContent: List[B],
                             initPanels: List[TextFieldComboPanel[B]],
                             minus: Minus = NO_EMPTY,
                             plus: Plus = ADD) extends MultiPanel(title,
  new TextFieldComboFactory(comboContent),
  initPanels,
  minus,
  plus)
