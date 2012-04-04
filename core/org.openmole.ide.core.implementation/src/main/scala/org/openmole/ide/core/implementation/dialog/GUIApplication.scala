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

package org.openmole.ide.core.implementation.dialog

import java.awt.Font
import javax.swing.UIManager

class GUIApplication { application => 
  
  val font = new Font("Ubuntu",Font.PLAIN,12)
  UIManager.put("Menu.font",font)
  UIManager.put("MenuItem.font",font)
  UIManager.put("Button.font",font)
  UIManager.put("Label.font",font)
  
  val frame = new GUIPanel {override def closeOperation = {
      super.closeOperation
      application.closeOperation
    }
  }
  
  def display = frame.visible = true
  
  def closeOperation : Unit = {}
}
