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

import scala.swing.ComboBox

object MultiTwoCombos {
  def twoCombosRowWidgetFactory[A,B](row: TwoCombosRowWidget[A,B]) = {
    import row._
    new TwoCombosRowWidget(comboContentA,selectedA,comboContentB,selectedB)
  }
  
  class TwoCombosRowWidget[A,B](val comboContentA: List[A], 
                                val selectedA: A, 
                                val comboContentB: List[B], 
                                val selectedB: B) extends IRowWidget2[A,B]{
    val combo1 = new ComboBox[A](comboContentA) {selection.item = selectedA}
    val combo2 = new ComboBox[B](comboContentB) {selection.item = selectedB}
    override val components = List(combo1,combo2)
  
    override def content: (A,B) = (combo1.selection.item,combo2.selection.item)
  }
}
import MultiTwoCombos._
class MultiTwoCombos[A,B](rowName: String, initValues: (List[A],List[B]), selected: List[(A,B)]) extends
MultiWidget(rowName, if (selected.isEmpty) List(new TwoCombosRowWidget(initValues._1,initValues._1(0), initValues._2, initValues._2(0)))
             else selected.map{case(s1,s2)=>new TwoCombosRowWidget(initValues._1, s1, initValues._2, s2)}, 
             twoCombosRowWidgetFactory[A,B],
             2){ def content = rowWidgets.map(_.content).toList }