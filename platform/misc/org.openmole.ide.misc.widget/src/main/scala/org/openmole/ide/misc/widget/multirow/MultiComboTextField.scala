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
import scala.swing.TextField

object MultiComboTextField {
  class Factory[A] extends IRowWidgetFactory[ComboTextFieldRowWidget[A]]{
    def apply(row: ComboTextFieldRowWidget[A]) = {
      import row._
      new ComboTextFieldRowWidget(comboContentA,selectedA,"")
    }
  }
  
  class ComboTextFieldRowWidget[A](val comboContentA: List[A],
                                   val selectedA: A,
                                   val initValue: String) extends IRowWidget2[A,String]{
    val textFied = new TextField(initValue,10)
    val comboBox = new ComboBox(comboContentA) {selection.item = selectedA}
    override val components = List(comboBox,textFied)
  
    override def content: (A,String) = (comboBox.selection.item,textFied.text)
  }
}

import MultiComboTextField._
class MultiComboTextField[A] (rowName: String,
                              initValues: List[(A,String)],
                              comboContent: List[A],
                              factory: IRowWidgetFactory[ComboTextFieldRowWidget[A]]) extends MultiWidget(
  rowName,
  if (initValues.isEmpty) 
    List(new ComboTextFieldRowWidget(comboContent, 
                                     comboContent(0),
                                     ""))
  else initValues.map{
    case(a,s)=>new ComboTextFieldRowWidget(comboContent,a,s)},
  factory,
  2)
{
  def this(rName: String,
           iValues: List[(A,String)],
           cContent: List[A]) = this (rName,iValues,cContent, new Factory[A])

  def content = rowWidgets.map(_.content).filterNot(_._2.isEmpty).toList 
}