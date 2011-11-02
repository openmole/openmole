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
import scala.swing.Component

object MultiTwoCombos {
  class TwoCombosRowWidget[A,B](comboContentA: List[A], selectedA: A, comboContentB: List[B], selectedB: B) extends IRowWidget{
    override val components = List(new ComboBox(comboContentA) {selection.item = selectedA},
                                   new ComboBox(comboContentB) {selection.item = selectedB})
  
    override def buildEmptyRow: IRowWidget = new TwoCombosRowWidget(comboContentA,selectedA,comboContentB,selectedB)
  
    override def content: List[(Component,_)] = components.map(c => (c, c.selection.item)).toList
  }
}

class MultiTwoCombos[A,B](rowName: String, initValues: (List[A],List[B]), selected: List[(A,B)]) extends
MultiWidget(rowName, if (selected.isEmpty) List(new MultiTwoCombos.TwoCombosRowWidget(initValues._1,initValues._1(0), initValues._2, initValues._2(0)))
            else selected.map{case(s1,s2)=>new MultiTwoCombos.TwoCombosRowWidget(initValues._1, s1, initValues._2, s2)}, 2)