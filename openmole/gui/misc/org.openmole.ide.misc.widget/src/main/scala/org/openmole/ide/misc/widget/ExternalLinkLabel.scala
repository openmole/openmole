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

import java.awt.Cursor
import java.awt.Desktop
import java.net.URI
import org.openmole.misc.exception.UserBadDataError
import scala.swing.Label
import scala.swing.event.MousePressed
import java.io.IOException

class ExternalLinkLabel(val textLink: String,
                        val href: String,
                        val textSize: Int = 4,
                        color: String = "#bbc807ff",
                        bold: Boolean = false) extends Label {
  cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  link(textLink)

  listenTo(this.mouse.clicks)
  reactions += {
    case e: MousePressed ⇒
      Desktop.isDesktopSupported match {
        case true  ⇒ Desktop.getDesktop.browse(new URI(href))
        case false ⇒ throw new UserBadDataError("Open your web browser to: " + href)
      }
  }

  def link(t: String) = text = "<html>" + {
    if (bold) "<b>" else ""
  } +
    "<font color=\"" + color + "\" size=\"" + textSize + "\">" +
    t +
    "</font>" + {
      if (bold) "</b>" else ""
    } +
    "</html>"

}
