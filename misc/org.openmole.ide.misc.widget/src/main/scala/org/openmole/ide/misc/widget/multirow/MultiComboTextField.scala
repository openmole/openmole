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

import org.openmole.ide.misc.widget.MyPanel
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget.Plus
import scala.swing.MyComboBox
import scala.swing.TextField

object MultiComboTextField {
  class Factory[A] extends IRowWidgetFactory[ComboTextFieldRowWidget[A]]{
    def apply(row: ComboTextFieldRowWidget[A], panel: MyPanel) = {
      import row._
      new ComboTextFieldRowWidget(comboContentA,selectedA,"",plus)
    }
  }
  
  class ComboTextFieldRowWidget[A](val comboContentA: List[A],
                                   val selectedA: A,
                                   val initValue: String,
                                   val plus: Plus) extends IRowWidget2[A,String]{
    val textFied = new TextField(initValue,10)
    val comboBox = new MyComboBox(comboContentA) 
    comboBox.selection.item = selectedA
    override val panel = new RowPanel(List(comboBox,textFied),plus)
    
    override def content: (A,String) = (comboBox.selection.item,textFied.text)
  }
}

import MultiComboTextField._

class MultiComboTextField[A](title: String,
                             rWidgets: List[ComboTextFieldRowWidget[A]], 
                             factory: IRowWidgetFactory[ComboTextFieldRowWidget[A]],
                             minus: Minus= NO_EMPTY,
                             plus: Plus= ADD) extends MultiWidget(title,rWidgets,factory,2,minus){

  def this(title: String,
           initValues: List[(A,String)],
           comboContent: List[A],
           factory: IRowWidgetFactory[ComboTextFieldRowWidget[A]],
           minus: Minus,
           plus: Plus) = this(title,
                              if (initValues.isEmpty) List(new ComboTextFieldRowWidget(comboContent, 
                                                                                       comboContent(0),
                                                                                       "",
                                                                                       plus))
                              else initValues.map{
      case(a,s)=>new ComboTextFieldRowWidget(comboContent,a,s,plus)},
                              factory,minus,plus)
  def this(title: String,
           iValues: List[(A,String)],
           cContent: List[A]) = this (title,iValues,cContent, new Factory[A],NO_EMPTY,ADD)

  def content = rowWidgets.map(_.content).toList
}
