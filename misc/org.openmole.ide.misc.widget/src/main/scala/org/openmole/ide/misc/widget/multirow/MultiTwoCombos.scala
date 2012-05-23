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
import org.openmole.ide.misc.widget.multirow.RowWidget.Plus
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.Label
import scala.swing.MyComboBox

object MultiTwoCombos {

  class Factory[A, B] extends IRowWidgetFactory[TwoCombosRowWidget[A, B]] {
    def apply(row: TwoCombosRowWidget[A, B], panel: MyPanel) = {
      import row._
      new TwoCombosRowWidget(comboContentA, selectedA, comboContentB, selectedB, inBetweenString, plus)

    }
  }

  class TwoCombosRowWidget[A, B](val comboContentA: List[A],
                                 val selectedA: A,
                                 val comboContentB: List[B],
                                 val selectedB: B,
                                 val inBetweenString: String,
                                 val plus: Plus) extends IRowWidget2[A, B] {

    val combo1 = new MyComboBox[A](comboContentA.sortBy { _.toString }) { selection.item = selectedA }
    val combo2 = new MyComboBox[B](comboContentB.sortBy { _.toString }) { selection.item = selectedB }

    override val panel = new RowPanel(List(combo1, new Label(inBetweenString), combo2), plus)

    override def content: (A, B) = (combo1.selection.item, combo2.selection.item)

  }
}

import MultiTwoCombos._
class MultiTwoCombos[A, B](title: String,
                           rWidgets: List[TwoCombosRowWidget[A, B]],
                           factory: IRowWidgetFactory[TwoCombosRowWidget[A, B]],
                           minus: Minus,
                           plus: Plus,
                           buildFromFactory: Boolean)
    extends MultiWidget(title, rWidgets, factory, minus, buildFromFactory) {
  def this(title: String,
           inbetweenString: String,
           initValues: (List[A], List[B]),
           selected: List[(A, B)],
           factory: IRowWidgetFactory[TwoCombosRowWidget[A, B]],
           minus: Minus = NO_EMPTY,
           plus: Plus = ADD,
           buildFromFactory: Boolean = false) = this(title,
    if (selected.isEmpty) {
      List(new TwoCombosRowWidget(initValues._1,
        initValues._1(0),
        initValues._2,
        initValues._2(0),
        inbetweenString,
        plus))
    } else
      selected.map {
        case (s1, s2) ⇒ new TwoCombosRowWidget(initValues._1,
          s1,
          initValues._2,
          s2,
          inbetweenString,
          plus)
      },
    factory, minus, plus, buildFromFactory)

  def this(title: String,
           ibString: String,
           iValues: (List[A], List[B]),
           selected: List[(A, B)],
           minus: Minus = NO_EMPTY,
           plus: Plus = ADD,
           buildFromFactory: Boolean = false) = this(title,
    ibString,
    iValues,
    selected,
    new Factory[A, B],
    minus,
    plus,
    buildFromFactory)

  //  def this(title: String,
  //           ibString: String,
  //            iValues: (List[A],List[B]), 
  //           selected: List[(A,B)],
  //           minus: Minus = NO_EMPTY, 
  //           plus: Plus = ADD,
  //           buildFromFactory : Boolean = false) = this(title,
  //                                              ibString,
  //                                              iValues,
  //                                              selected, 
  //                                              new Factory[A,B],
  //                                              minus,
  //                                              plus,
  //                                              buildFromFactory)
  def content = rowWidgets.map(_.content).toList
}