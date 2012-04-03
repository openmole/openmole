/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) _ later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT _ WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.misc.widget.multirow

import javax.swing.Icon
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.MyPanel
import org.openmole.ide.misc.widget.PrototypeGroovyTextFieldEditor
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget.Plus
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.ComboBox
import scala.swing.event.SelectionChanged

object MultiComboLinkLabelGroovyTextFieldEditor {
  class Factory[A] extends IRowWidgetFactory[ComboLinkLabelGroovyTextFieldEditorRowWidget[A]]{
    def apply(row: ComboLinkLabelGroovyTextFieldEditorRowWidget[A], panel: MyPanel) = {
      import row._
      new ComboLinkLabelGroovyTextFieldEditorRowWidget(comboContent,initValues,image,plus)
    }
  }
 
  class ComboLinkLabelGroovyTextFieldEditorRowWidget[A](val comboContent : List[(A,IPrototype[_],ContentAction[A])],
                                                        val initValues : (A,IPrototype[_],ContentAction[A],String),
                                                        val image : Icon,
                                                        val plus: Plus) extends IRowWidget2[A,String]{
    val linkLabel = new LinkLabel("",initValues._3) {icon = image}
    var textField = new PrototypeGroovyTextFieldEditor("Default value",initValues._2,initValues._4)
    
    val comboBox = new ComboBox(comboContent.map(c=>c._1)) 
    comboBox.selection.item = initValues._1
    
    override val panel = 
      new RowPanel(List(comboBox,linkLabel,textField),plus){    
        listenTo(`comboBox`)
        comboBox.selection.reactions += {
          case SelectionChanged(`comboBox`)=> 
            val it = comboContent.filter{cc => cc._1 == comboBox.selection.item}.head
            linkLabel.action = it._3
            contents(0) match {
              case x : MigPanel => 
                x.contents.remove(2)
                textField = new PrototypeGroovyTextFieldEditor("Default value",it._2,"")
                x.contents.insert(2,textField)
            }
        }
      }
    
    override def content: (A,String) = (comboBox.selection.item,textField.editorText)
  }
}

import MultiComboLinkLabelGroovyTextFieldEditor._

class MultiComboLinkLabelGroovyTextFieldEditor[A](title: String,
                                                  rWidgets: List[ComboLinkLabelGroovyTextFieldEditorRowWidget[A]], 
                                                  factory: IRowWidgetFactory[ComboLinkLabelGroovyTextFieldEditorRowWidget[A]],
                                                  minus: Minus= CLOSE_IF_EMPTY,
                                                  plus: Plus= ADD) extends MultiWidget(title,rWidgets,factory,2,minus){

  
  def this(title: String,
           comboContent: List[(A,IPrototype[_],ContentAction[A])],
           initValues: List[(A,IPrototype[_],ContentAction[A],String)],
           image : Icon,
           factory: IRowWidgetFactory[ComboLinkLabelGroovyTextFieldEditorRowWidget[A]],
           minus: Minus,
           plus: Plus) = this(title,
                              if (initValues.isEmpty) {List(new ComboLinkLabelGroovyTextFieldEditorRowWidget(comboContent,
                                                                                                             (comboContent(0)._1,comboContent(0)._2,comboContent(0)._3,""),
                                                                                                             image,
                                                                                                             plus))
    }
                              else initValues.map{
      case(a,m,action,s)=>new ComboLinkLabelGroovyTextFieldEditorRowWidget(comboContent,(a,m,action,s),image,plus)}
                              ,factory,minus,plus)
  def this(title: String,
           initValues: List[(A,IPrototype[_],ContentAction[A],String)],
           comboContent: List[(A,IPrototype[_],ContentAction[A])],
           image : Icon) = this(title,
                                comboContent,
                                initValues,
                                image,
                                new Factory[A],CLOSE_IF_EMPTY,ADD)
           
  def content = rowWidgets.map(_.content).toList 
}