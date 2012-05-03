/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

import org.openmole.ide.misc.widget.MyPanel
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.MyComboBox
import scala.swing.TextField

object MultiTextFieldCombo {
  class Factory[B] extends IRowWidgetFactory[TextFieldComboRowWidget[B]] {
    def apply(row: TextFieldComboRowWidget[B], panel: MyPanel) = {
      import row._
      new TextFieldComboRowWidget("", comboContentB, selectedB)
    }
  }

  class TextFieldComboRowWidget[B](val initValue: String,
                                   val comboContentB: List[B],
                                   val selectedB: B) extends IRowWidget2[String, B] {
    val textFied = new TextField(initValue, 10)
    val comboBox = new MyComboBox(comboContentB) { selection.item = selectedB }
    override val panel = new RowPanel(List(textFied, comboBox))

    override def content: (String, B) = (textFied.text, comboBox.selection.item)
  }
}

import MultiTextFieldCombo._
class MultiTextFieldCombo[B](title: String,
                             initValues: List[(String, B)],
                             comboContent: List[B],
                             factory: IRowWidgetFactory[TextFieldComboRowWidget[B]],
                             minus: Minus) extends MultiWidget(title,
  if (initValues.isEmpty)
    List(new TextFieldComboRowWidget("",
    comboContent,
    comboContent(0)))
  else initValues.map {
    case (s, b) ⇒ new TextFieldComboRowWidget(s, comboContent, b)
  },
  factory,
  2, minus) {

  def this(title: String,
           iValues: List[(String, B)],
           cContent: List[B]) = this(title,
    iValues,
    cContent,
    new Factory[B],
    NO_EMPTY)

  def content = rowWidgets.map(_.content).toList
}