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

import java.awt.Dimension
import org.openmole.ide.misc.widget.MyPanel
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.MyComboBox

object MultiCombo {

  class Factory[A] extends IRowWidgetFactory[ComboRowWidget[A]] {
    def apply(row: ComboRowWidget[A], panel: MyPanel) = {
      import row._
      new ComboRowWidget(comboContentA, selectedA, plus)
    }
  }

  class ComboRowWidget[A](val comboContentA: List[A],
                          val selectedA: A,
                          val plus: Plus) extends IRowWidget1[A] {

    val combo = new MyComboBox(comboContentA) { selection.item = selectedA; preferredSize = new Dimension(8, size.height) }

    override val panel = new RowPanel(List(combo), plus)

    override def content: A = combo.selection.item

  }
}

import MultiCombo._
class MultiCombo[A](title: String,
                    rWidgets: List[ComboRowWidget[A]],
                    factory: IRowWidgetFactory[ComboRowWidget[A]],
                    minus: Minus = CLOSE_IF_EMPTY,
                    plus: Plus = ADD)
    extends MultiWidget(title, rWidgets, factory, minus) {
  def this(title: String,
           initValues: List[A],
           selected: List[A],
           factory: IRowWidgetFactory[ComboRowWidget[A]],
           minus: Minus,
           plus: Plus) = this(title,
    if (selected.isEmpty) {
      List(new ComboRowWidget(initValues,
        initValues(0),
        plus))
    } else
      selected.map { s1 ⇒
        new ComboRowWidget(initValues,
          s1,
          plus)
      },
    factory, minus, plus)

  def this(title: String,
           iValues: List[A],
           selected: List[A]) = this(title,
    iValues,
    selected,
    new Factory[A],
    NO_EMPTY,
    ADD)

  def this(title: String,
           iValues: List[A],
           selected: List[A],
           minus: Minus,
           plus: Plus) = this(title,
    iValues,
    selected,
    new Factory[A],
    minus,
    plus)
  def content = rowWidgets.map(_.content).toList
}
