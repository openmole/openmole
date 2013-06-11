/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.execution

import java.awt.Color
import scala.swing.Button
import scala.swing.Label
import scala.swing.PasswordField
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.misc.workspace.Workspace
import scala.swing.event.KeyPressed
import scala.swing.event.ButtonClicked
import scala.swing.event.Key._
import scala.swing.event.KeyReleased
import scala.swing.event.MousePressed

object PasswordDialog {

  val passField = new PasswordField(12) {
    listenTo(keys)
    reactions += {
      case KeyPressed(_, Enter, _, _) ⇒ ok(true)
      case KeyReleased(_, _, _, _)    ⇒ testPassword
    }
  }

  val initButton = new Button("Reset")

  val panel = new MigPanel("") {
    contents += new Label("Password: ")
    contents += passField
    contents += initButton
  }

  panel.listenTo(`initButton`)
  panel.reactions += {
    case ButtonClicked(`initButton`) ⇒
      if (DialogFactory.changePasswordConfirmation)
        Workspace.reset
  }

  private def setColor(c: Color) = {
    passField.foreground = c
    passField.repaint
  }

  def testPassword: Boolean = {
    if (Workspace.passwordIsCorrect(new String(passField.password))) {
      setColor(new Color(136, 170, 0))
      true
    }
    else {
      setColor(new Color(212, 0, 0))
      false
    }
  }

  def ok(b: Boolean): Unit = if (b && testPassword) Workspace.setPassword(new String(passField.password))

}