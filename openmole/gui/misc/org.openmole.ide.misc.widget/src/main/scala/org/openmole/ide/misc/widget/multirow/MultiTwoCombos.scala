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
import org.openmole.ide.misc.widget.multirow.RowWidget.Plus
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.Label
import scala.swing.MyComboBox

object MultiTwoCombos {

  class TwoCombosPanel[A, B](comboContent1: Seq[A],
                             comboContent2: Seq[B],
                             inBetweenString: String,
                             data: TwoCombosData[A, B]) extends PluginPanel("wrap 3") with IPanel[TwoCombosData[A, B]] {

    val comboBox1 = new MyComboBox(comboContent1.sortBy { _.toString }) {
      data.comboValue1 match {
        case Some(x: A) ⇒ selection.item = x
        case _          ⇒
      }
    }

    val comboBox2 = new MyComboBox(comboContent2.sortBy { _.toString }) {
      data.comboValue2 match {
        case Some(x: B) ⇒ selection.item = x
        case _          ⇒
      }
    }

    contents += comboBox1
    contents += new Label(inBetweenString)
    contents += comboBox2

    def content = new TwoCombosData(Some(comboBox1.selection.item),
      Some(comboBox2.selection.item))
  }

  class TwoCombosData[A, B](val comboValue1: Option[A] = None,
                            val comboValue2: Option[B] = None) extends IData

  class TwoCombosFactory[A, B](comboContent1: Seq[A],
                               comboContent2: Seq[B],
                               inBetweenString: String) extends IFactory[TwoCombosData[A, B]] {
    def apply = new TwoCombosPanel(comboContent1,
      comboContent2,
      inBetweenString,
      new TwoCombosData)
  }
}

import MultiTwoCombos._
class MultiTwoCombos[A, B](title: String,
                           comboContent1: Seq[A],
                           comboContent2: Seq[B],
                           inBetweenString: String,
                           initPanels: Seq[TwoCombosPanel[A, B]],
                           minus: Minus = NO_EMPTY,
                           plus: Plus = ADD) extends MultiPanel(title,
  new TwoCombosFactory(comboContent1, comboContent2, inBetweenString),
  initPanels,
  minus,
  plus)
