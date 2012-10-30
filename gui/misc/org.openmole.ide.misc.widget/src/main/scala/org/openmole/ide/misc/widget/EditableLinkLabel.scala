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

package org.openmole.ide.misc.widget

import java.awt.Color
import java.awt.Cursor
import scala.swing.Label
import scala.swing.MenuItem
import scala.swing.event.MousePressed

class EditableLinkLabel[A](val action: ContentAction[A],
                           choices: List[ContentAction[A]],
                           val textSize: Int = 4) extends Label {
  foreground = Color.white
  cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  val popup = new PopupMenu
  choices.foreach { popup.contents += new LinkLabelMenuItem(_, this) }
  link(action.title)

  listenTo(mouse.clicks)
  reactions += {
    case e: MousePressed ⇒
      popup.isOpened match {
        case true ⇒ popup.hide
        case false ⇒ popup.show(this, 0, size.height)
      }
  }

  def link(t: String) = text = "<html><font color=\"#507698\" size=\"" + textSize + "\">" + t + "</font></html>"

  class LinkLabelMenuItem(a: ContentAction[A],
                          lab: EditableLinkLabel[A]) extends MenuItem(a) {
    listenTo(mouse.clicks)
    reactions += {
      case e: MousePressed ⇒ lab.link(a.title)
    }
  }
}