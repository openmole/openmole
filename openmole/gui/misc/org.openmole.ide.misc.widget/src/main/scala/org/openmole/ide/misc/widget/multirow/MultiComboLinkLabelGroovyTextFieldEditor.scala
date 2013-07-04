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
import org.openmole.core.model.data._
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.PrototypeGroovyTextFieldEditor
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget.Plus
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.MyComboBox
import scala.swing.event.SelectionChanged

object MultiComboLinkLabelGroovyTextFieldEditor {

  class ComboLinkLabelGroovyTextFieldEditorPanel[A](val comboContent: Seq[(A, Prototype[_], ContentAction[A])],
                                                    val image: Icon,
                                                    val data: ComboLinkLabelGroovyTextFieldEditorData[A]) extends PluginPanel("wrap 3") with IPanel[ComboLinkLabelGroovyTextFieldEditorData[A]] {
    val filterComboBox = FilterComboBox(comboContent.sortBy { _._1.toString }.map(c ⇒ c._1))
    data.content match {
      case Some(x: A) ⇒
        filterComboBox.combo.selection.item = x
      case _ ⇒
    }

    var textField = new PrototypeGroovyTextFieldEditor("Value for " + data.prototype.name, data.prototype, data.editorValue)
    val linkLabel = new LinkLabel("", comboTuple._3) { icon = image }

    contents += filterComboBox
    contents += linkLabel
    contents += textField

    listenTo(filterComboBox.combo)
    filterComboBox.combo.selection.reactions += {
      case SelectionChanged(filterComboBox.combo) ⇒
        val tuple = comboTuple
        linkLabel.action = tuple._3
        contents(2) match {
          case x: PrototypeGroovyTextFieldEditor ⇒
            contents.remove(2)
            textField = new PrototypeGroovyTextFieldEditor("Default value", tuple._2, "")
            contents.insert(2, textField)
          case _ ⇒
        }
    }

    def comboTuple = comboContent.filter { cc ⇒ cc._1 == filterComboBox.combo.selection.item }.head

    def content = {
      val tuple = comboTuple
      new ComboLinkLabelGroovyTextFieldEditorData(tuple._2, Some(tuple._1), textField.editorText)
    }
  }

  class ComboLinkLabelGroovyTextFieldEditorData[A](val prototype: Prototype[_],
                                                   val content: Option[A] = None,
                                                   val editorValue: String = "") extends IData

  class ComboLinkLabelGroovyTextFieldEditorFactory[A](comboContent: Seq[(A, Prototype[_], ContentAction[A])],
                                                      image: Icon) extends IFactory[ComboLinkLabelGroovyTextFieldEditorData[A]] {
    def apply =
      new ComboLinkLabelGroovyTextFieldEditorPanel(comboContent,
        image,
        new ComboLinkLabelGroovyTextFieldEditorData(comboContent(0)._2,
          Some(comboContent(0)._1),
          ""))
  }
}

import MultiComboLinkLabelGroovyTextFieldEditor._
class MultiComboLinkLabelGroovyTextFieldEditor[A](title: String,
                                                  comboContent: Seq[(A, Prototype[_], ContentAction[A])],
                                                  initPanels: Seq[ComboLinkLabelGroovyTextFieldEditorPanel[A]],
                                                  image: Icon,
                                                  minus: Minus = NO_EMPTY,
                                                  plus: Plus = ADD,
                                                  insets: RowInsets = REGULAR) extends MultiPanel(title,
  new ComboLinkLabelGroovyTextFieldEditorFactory(comboContent, image),
  initPanels,
  minus,
  plus,
  insets)