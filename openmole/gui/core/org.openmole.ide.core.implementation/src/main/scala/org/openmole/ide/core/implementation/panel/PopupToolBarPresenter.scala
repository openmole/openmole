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

package org.openmole.ide.core.implementation.panel

import org.openmole.ide.misc.widget.PopupMenu
import scala.swing.Component
import scala.swing.Menu
import scala.swing.MenuItem
import scala.swing.Button
import scala.swing.MenuItem
import scala.swing.event.ButtonClicked
import java.awt.Color
import org.openmole.ide.misc.tools.image.Images._

class PopupToolBarPresenter(t: String,
                            basemenu: List[MenuItem],
                            bgColor: Color,
                            fgColor: Color = Color.WHITE) extends Button(t) {
  val popup = new PopupMenu { basemenu.foreach { contents += } }

  icon = ARROW
  background = bgColor
  foreground = fgColor

  listenTo(mouse.clicks)
  listenTo(basemenu.toSeq: _*)

  reactions += {
    case x: ButtonClicked ⇒ popup.show(this, 0, size.height)
    case ConceptChanged(item) ⇒ {
      text = item.text
      popup.hide
      revalidate
      repaint
    }
  }

  def remove(c: Component) = c match {
    case x: Menu     ⇒
    case x: MenuItem ⇒ popup.contents -= c
  }

  def removeAll = {
    popup.peer.removeAll
    basemenu.foreach { popup.contents += }
  }

}