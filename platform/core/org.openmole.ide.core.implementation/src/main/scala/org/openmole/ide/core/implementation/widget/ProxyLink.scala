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

package org.openmole.ide.core.implementation

import java.awt.Cursor
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import scala.swing.Label
import scala.swing.event.MousePressed

class ProxyLink(proxy: IDataProxyUI,textSize: Int = 4) extends Label {

  cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  link(proxy.dataUI.displayName)
  
  listenTo(this.mouse.clicks)
  reactions += {
    case e: MousePressed => 
  }
  
  def link(t: String) = text = "<html><font color=\"#0000CF\" size=\""+textSize+"\">"+t+"</font></html>"
  
  
}
