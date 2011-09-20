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

package org.openmole.ide.core.implementation.control

import scala.swing.Button
import scala.swing.Dialog
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JButton
import javax.swing.JDialog
import scala.swing.Label
import scala.swing.PasswordField
import net.miginfocom.swing.MigLayout
import org.openide.windows.WindowManager
import org.openmole.misc.workspace.Workspace
import scala.swing.event.KeyPressed
import scala.swing.event.Key._
import scala.swing.event.KeyReleased

object PasswordDialog extends JDialog(WindowManager.getDefault.getMainWindow){
  
  setTitle("Preference access")
  val passField = new PasswordField(12){
    listenTo(keys)
    reactions += {
      case KeyPressed(_, Enter, _, _) => ok
      case KeyReleased(_, _, _, _) => testPassword}}
  val okButton = new JButton("OK")
  val cancelButton = new JButton("Cancel")
  setLayout(new MigLayout(""))
  add(new Label("Password: ").peer)
  add(passField.peer)
  add(okButton)
  add(cancelButton)
  
  setModal(true)
  setMinimumSize(new Dimension(350,80))
  
  okButton.addActionListener(new ActionListener {
      override def actionPerformed(ae: ActionEvent) = {ok}})
    
  cancelButton.addActionListener(new ActionListener {
      override def actionPerformed(ae: ActionEvent) = {
        setVisible(false)}})

  setLocationRelativeTo(WindowManager.getDefault.getMainWindow)

  private def setColor(c: Color) = {
    passField.foreground = c
    passField.repaint
  }
  
  def testPassword: Boolean = {
    if (Workspace.passwordIsCorrect(new String(passField.password))) {
      setColor(new Color(136,170,0) )
      true
    } else {
      setColor(new Color(212,0,0))
      false}
  }
  
  def ok:Unit = { if (testPassword) {
      Workspace.password_=(new String(passField.password))
      setVisible(false)}
  }
  setVisible(true)
}