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

//
//object MultiTextFieldCombo {
//  class Factory[B] extends IRowWidgetFactory[TextFieldComboRowWidget[B]] {
//    def apply(row: TextFieldComboRowWidget[B], panel: MyPanel) = {
//      import row._
//      new TextFieldComboRowWidget("", comboContentB, selectedB)
//    }
//  }
//
//  class TextFieldComboRowWidget[B](val initValue: String,
//                                   val comboContentB: List[B],
//                                   val selectedB: B) extends IRowWidget2[String, B] {
//    val textFied = new TextField(initValue, 10)
//    val comboBox = new MyComboBox(comboContentB.sortBy { _.toString }) { selection.item = selectedB }
//    override val panel = new RowPanel(List(textFied, comboBox))
//
//    override def content: (String, B) = (textFied.text, comboBox.selection.item)
//  }
//}
//
//import MultiTextFieldCombo._
//class MultiTextFieldCombo[B](title: String,
//                             initValues: List[(String, B)],
//                             comboContent: List[B],
//                             factory: IRowWidgetFactory[TextFieldComboRowWidget[B]],
//                             minus: Minus = NO_EMPTY) extends MultiWidget(title,
//  if (initValues.isEmpty)
//    List(new TextFieldComboRowWidget("",
//    comboContent,
//    comboContent(0)))
//  else initValues.map {
//    case (s, b) ⇒ new TextFieldComboRowWidget(s, comboContent, b)
//  },
//  factory,
//  minus) {
//
//  def this(title: String,
//           iValues: List[(String, B)],
//           cContent: List[B],
//           minus: Minus = NO_EMPTY) = this(title,
//    iValues,
//    cContent,
//    new Factory[B],
//    minus)
//
//  def content = rowWidgets.map(_.content).toList
//}