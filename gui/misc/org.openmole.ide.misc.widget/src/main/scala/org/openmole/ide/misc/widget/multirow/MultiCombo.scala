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

import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.MyComboBox

object MultiCombo {

  class ComboPanel[B](val comboContent: List[B],
                      val data: ComboData[B]) extends PluginPanel("wrap 2") with IPanel[ComboData[B]] {

    val comboBox = new MyComboBox(comboContent.sortBy { _.toString }) {
      data.comboValue match {
        case Some(x: B) ⇒ selection.item = x
        case _ ⇒
      }
    }

    contents += comboBox

    def content = new ComboData(Some(comboBox.selection.item))
  }

  class ComboData[B](val comboValue: Option[B] = None) extends IData

  class ComboFactory[B](comboContent: List[B]) extends IFactory[ComboData[B]] {
    def apply = new ComboPanel(comboContent, new ComboData)
  }
}

import MultiCombo._
class MultiCombo[B](title: String,
                    comboContent: List[B],
                    initPanels: List[ComboPanel[B]],
                    minus: Minus = NO_EMPTY,
                    plus: Plus = ADD) extends MultiPanel(title,
  new ComboFactory(comboContent),
  initPanels,
  minus,
  plus)