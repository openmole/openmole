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
import scala.swing.TextField

object MultiTextField {
  class Factory extends IRowWidgetFactory[TextFieldRowWidget] {
    def apply(row: TextFieldRowWidget, panel: MyPanel) = {
      import row._
      new TextFieldRowWidget(initValue)
    }
  }

  class TextFieldRowWidget(val initValue: String) extends IRowWidget1[String] {
    val textField = new TextField(initValue, 10)
    override val panel = new RowPanel(List(textField))

    override def content: String = textField.text
  }
}

import MultiTextField._
class MultiTextField(title: String,
                     initValues: List[String],
                     factory: IRowWidgetFactory[TextFieldRowWidget],
                     minus: Minus) extends MultiWidget(title,
  if (initValues.isEmpty)
    List(new TextFieldRowWidget(""))
  else initValues.map { s ⇒ new TextFieldRowWidget(s) },
  factory,
  minus) {

  def this(title: String,
           iValues: List[String]) = this(title,
    iValues,
    new Factory,
    NO_EMPTY)

  def content = rowWidgets.map(_.content).toList
}