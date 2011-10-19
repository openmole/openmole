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

package org.openmole.ide.core.implementation.preference

import javax.swing.JDialog
import org.openmole.ide.misc.widget.MigPanel
import scala.swing.Button
import scala.swing.Component
import scala.swing.TabbedPane
import scala.swing.event.ButtonClicked

class PreferenceContent(frame: JDialog) extends MigPanel("wrap","[right]",""){ 
  val applyButton = new Button("Apply")
  val authentification = new AuthentificationPanel
  listenTo(applyButton)
  reactions += {case ButtonClicked(`applyButton`) =>  save}
    contents+= new TabbedPane {
      pages.append(new TabbedPane.Page("Authentification",authentification))
    }
    contents+= applyButton
  
  
  def save = {
    authentification.save
    frame.setVisible(false)
  }
}
