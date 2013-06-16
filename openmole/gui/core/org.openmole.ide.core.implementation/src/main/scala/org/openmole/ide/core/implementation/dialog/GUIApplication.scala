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

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Frame
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource
import org.openmole.ide.misc.tools.image.Images._
import org.openmole.ide.core.implementation.action.SaveXML
import java.util.concurrent.Semaphore

class GUIApplication {
  application ⇒

  val font = new Font("Ubuntu", Font.PLAIN, 12)
  val fontBold = new Font("Ubuntu", Font.BOLD, 12)
  UIManager.put("Menu.font", fontBold)
  UIManager.put("MenuItem.font", fontBold)
  UIManager.put("Button.font", fontBold)
  UIManager.put("text", new Color(77, 77, 77))
  UIManager.put("TabbedPane.foreground", new Color(0, 0, 255))
  UIManager.put("TabbedPane.font", fontBold)
  UIManager.put("Button.foreground", new ColorUIResource(new Color(77, 77, 77)))
  UIManager.put("Label.font", font)

  val frame =
    new GUIPanel {
      import javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE
      peer.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)

      iconImage = APPLICATION_ICON.getImage

      override def closeOperation = {
        application.closeOperation
        super.closeOperation
      }
    }

  def display = {
    frame.minimumSize = new Dimension(600, 300)
    frame.peer.setExtendedState(Frame.MAXIMIZED_BOTH)
    frame.visible = true
  }

  def closeOperation: Unit =
    if (DialogFactory.confirmationDialog(" Exit OpenMOLE", "<html>You are exiting the OpenMOLE application.<br>Save the project ?</html>"))
      SaveXML.save(frame)
}
