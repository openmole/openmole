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
import scala.swing.Label
import scala.swing.Panel

object MultiTwoCombos {
  
  class Factory[A, B] extends IRowWidgetFactory[TwoCombosRowWidget[A,B]]{
    def apply(row: TwoCombosRowWidget[A,B], panel: Panel) = {
      import row._
      new TwoCombosRowWidget(name,comboContentA,selectedA,comboContentB,selectedB, inBetweenString)
      
    }
  }
  
  class TwoCombosRowWidget[A,B](override val name: String,
                                val comboContentA: List[A], 
                                val selectedA: A, 
                                val comboContentB: List[B], 
                                val selectedB: B,
                                val inBetweenString: String) extends IRowWidget2[A,B]{
    
    val combo1 = new ComboBox[A](comboContentA) { selection.item = selectedA }
    val combo2 = new ComboBox[B](comboContentB) { selection.item = selectedB }
    
    override val panel = new RowPanel(name,List(combo1,new Label(inBetweenString),combo2))
    
    // var components = List(combo1,new Label(inBetweenString),combo2)
    
    override def content: (A,B) = (combo1.selection.item,combo2.selection.item)
    
  }
}

import MultiTwoCombos._
class MultiTwoCombos[A,B](rWidgets: List[TwoCombosRowWidget[A,B]], 
                          factory: IRowWidgetFactory[TwoCombosRowWidget[A,B]]) 
extends MultiWidget(rWidgets,factory,3){ 
  def this(
    rowName: String,
    inbetweenString: String,
    initValues: (List[A],List[B]), 
    selected: List[(A,B)],
    factory: IRowWidgetFactory[TwoCombosRowWidget[A,B]]) = this (if (selected.isEmpty) { List(new TwoCombosRowWidget(rowName,
                                                                                                                     initValues._1,
                                                                                                                     initValues._1(0),
                                                                                                                     initValues._2, 
                                                                                                                     initValues._2(0),
                                                                                                                     inbetweenString))}
                                                                 else
                                                                   selected.map{case(s1,s2)=> new TwoCombosRowWidget(rowName,
                                                                                                                     initValues._1, 
                                                                                                                     s1,
                                                                                                                     initValues._2,
                                                                                                                     s2,
                                                                                                                     inbetweenString)}, 
                                                                 factory)

  def this(rName: String , ibString: String,iValues: (List[A],List[B]), selected: List[(A,B)]) = this(rName,
                                                                                                      ibString,
                                                                                                      iValues,
                                                                                                      selected, 
                                                                                                      new Factory[A,B])
  def content = rowWidgets.map(_.content).toList 
}