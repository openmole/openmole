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

import javax.swing.Icon
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget.Plus
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.Action
import scala.swing.ComboBox
import scala.swing.Panel
import scala.swing.event.SelectionChanged

object MultiComboLinkLabel {
  class Factory[A] extends IRowWidgetFactory[ComboLinkLabelRowWidget[A]]{
    def apply(row: ComboLinkLabelRowWidget[A], panel: Panel) = {
      import row._
      new ComboLinkLabelRowWidget(comboContent,initValue,image ,plus)
    }
  }
 
  class ComboLinkLabelRowWidget[A](val comboContent : List[(A,ContentAction[A])], 
                                   val initValue : (A,ContentAction[A]),
                                   val image : Icon,
                                   val plus: Plus) extends IRowWidget1[A]{
    val linkLabel = new LinkLabel("",initValue._2) {
      icon = image}
    val comboBox = new ComboBox(comboContent.map(c=>c._1))
    comboBox.selection.item = initValue._1
    override val panel = new RowPanel(List(comboBox,linkLabel),plus){
       listenTo(`comboBox`)
        comboBox.selection.reactions += {
          case SelectionChanged(`comboBox`)=> 
            linkLabel.action = {
              comboContent.filter{cc => cc._1 == comboBox.selection.item}.head._2
            }
        }
    }
    
    override def content: A = comboBox.selection.item
  }
}
import MultiComboLinkLabel._

class MultiComboLinkLabel[A](title: String,
                             rWidgets: List[ComboLinkLabelRowWidget[A]], 
                             factory: IRowWidgetFactory[ComboLinkLabelRowWidget[A]],
                             minus: Minus= CLOSE_IF_EMPTY,
                             plus: Plus= ADD) extends MultiWidget(title,rWidgets,factory,2,minus){
  def this(title: String,
           comboContent: List[(A,ContentAction[A])], 
           selected: List[(A,ContentAction[A])],
           image : Icon,
           factory: IRowWidgetFactory[ComboLinkLabelRowWidget[A]],
           minus: Minus,
           plus: Plus) = this(title,
                              if (selected.isEmpty) 
                                List(new ComboLinkLabelRowWidget(comboContent,
                                                                 (comboContent(0)._1,comboContent(0)._2),
                                                                 image,plus))
                              else selected.map{ s=> new ComboLinkLabelRowWidget(comboContent,s,image,plus)},factory,minus,plus)
  
  def this(title: String,
           selected: List[(A,ContentAction[A])],
           comboContent: List[(A,ContentAction[A])],
           image : Icon) = this(title,
                                comboContent,
                                selected,
                                image,
                                new Factory[A],CLOSE_IF_EMPTY,ADD)
           
  def content = rowWidgets.map(_.content).toList 
}