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
 import org.openmole.ide.misc.widget.multirow.MultiWidget._
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.misc.widget.multirow

import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget.Plus

import scala.swing.MyComboBox
import scala.swing.TextField

object MultiComboTextField {

  class ComboTextFieldPanel[B](val comboContent: List[B],
                               val data: ComboTextFieldData[B]) extends PluginPanel("wrap 2") with IPanel[ComboTextFieldData[B]] {

    val textField = new TextField(data.textFieldValue, 15)
    val comboBox = new MyComboBox(comboContent.sortBy { _.toString }) {
      data.comboValue match {
        case Some(x: B) ⇒ selection.item = x
        case _ ⇒
      }
    }

    contents += comboBox
    contents += textField

    def content = new ComboTextFieldData(Some(comboBox.selection.item), textField.text)
  }

  class ComboTextFieldData[B](val comboValue: Option[B] = None,
                              val textFieldValue: String = "") extends IData

  class ComboTextFieldFactory[B](comboContent: List[B]) extends IFactory[ComboTextFieldData[B]] {
    def apply = new ComboTextFieldPanel(comboContent, new ComboTextFieldData)
  }
}

import MultiComboTextField._
class MultiComboTextField[B](title: String,
                             comboContent: List[B],
                             initPanels: List[ComboTextFieldPanel[B]],
                             minus: Minus = NO_EMPTY,
                             plus: Plus = ADD) extends MultiPanel(title,
  new ComboTextFieldFactory(comboContent),
  initPanels,
  minus,
  plus)

//object MultiComboTextField {
//  class Factory[A] extends IRowWidgetFactory[ComboTextFieldRowWidget[A]] {
//    def apply(row: ComboTextFieldRowWidget[A], panel: MyPanel) = {
//      import row._
//      new ComboTextFieldRowWidget(comboContentA, selectedA, "", plus)
//    }
//  }
//
//  class ComboTextFieldRowWidget[A](val comboContentA: List[A],
//                                   val selectedA: A,
//                                   val initValue: String,
//                                   val plus: Plus) extends IRowWidget2[A, String] {
//    val textFied = new TextField(initValue, 10)
//    val comboBox = new MyComboBox(comboContentA.sortBy { _.toString })
//    comboBox.selection.item = selectedA
//    override val panel = new RowPanel(List(comboBox, textFied), plus)
//
//    override def content: (A, String) = (comboBox.selection.item, textFied.text)
//  }
//}
//
//import MultiComboTextField._
//
//class MultiComboTextField[A](title: String,
//                             rWidgets: List[ComboTextFieldRowWidget[A]],
//                             factory: IRowWidgetFactory[ComboTextFieldRowWidget[A]],
//                             minus: Minus,
//                             plus: Plus) extends MultiWidget(title, rWidgets, factory, minus) {
//
//  def this(title: String,
//           initValues: List[(A, String)],
//           comboContent: List[A],
//           factory: IRowWidgetFactory[ComboTextFieldRowWidget[A]],
//           minus: Minus = NO_EMPTY,
//           plus: Plus = ADD) = this(title,
//    if (initValues.isEmpty) List(new ComboTextFieldRowWidget(comboContent,
//      comboContent(0),
//      "",
//      plus))
//    else initValues.map {
//      case (a, s) ⇒ new ComboTextFieldRowWidget(comboContent, a, s, plus)
//    },
//    factory, minus, plus)
//  def this(title: String,
//           iValues: List[(A, String)],
//           cContent: List[A],
//           minus: Minus,
//           plus: Plus) = this(title, iValues, cContent, new Factory[A], minus, plus)
//
//  def content = rowWidgets.map(_.content).toList
//}
