/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.misc.widget

import scala.swing.MyComboBox

object ContentComboBox {
  def mapContent[T](rawContent: List[T]) = rawContent.map {
    p ⇒ new Content(Some(p))
  }

  def apply[T](rawContent: List[T], selectedItem: Option[T] = None) = {
    new ContentComboBox(rawContent, selectedItem)
  }
}

import ContentComboBox._

class ContentComboBox[T](rawContent: List[T], selectedItem: Option[T] = None) {
  val none = Content(None)
  val contents: List[Content[T]] = none :: mapContent(rawContent)
  val widget = new MyComboBox(contents)
  setSelection(selectedItem)

  def setModel(rawContent: List[T], sel: Option[T] = None) = {
    widget.peer.setModel(MyComboBox.newConstantModel(Content(None) :: mapContent(rawContent)))
    setSelection(sel)
  }

  def setSelection[T](sel: Option[T]) = {
    widget.selection.item = contents.find(_.content == sel).headOption match {
      case Some(x: Content[T]) ⇒ x
      case _                   ⇒ none
    }
  }
}

case class Content[+T](content: Option[T]) {
  override def toString = content match {
    case None    ⇒ "None"
    case Some(p) ⇒ p.toString
  }
}
