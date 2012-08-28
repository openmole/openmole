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