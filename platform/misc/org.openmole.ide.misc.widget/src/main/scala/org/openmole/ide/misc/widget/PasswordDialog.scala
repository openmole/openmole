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

package org.openmole.ide.misc.widget

import javax.swing.JOptionPane
import scala.swing.PasswordField

object PasswordDialog {
  def show = {
    val passField = new  PasswordField
    val jop = new JOptionPane(passField,JOptionPane.QUESTION_MESSAGE,JOptionPane.OK_CANCEL_OPTION)
    val dialog = jop.createDialog("Password:")
    dialog.setVisible(true)
    val result = jop.getValue
    dialog.dispose
    if(result == JOptionPane.OK_OPTION){
      println ("OK !!")
    }
  }
  // contents+= "aaaaaaaaaaaa"
//  new MigPanel("wrap 2") {
//    contents+= (new Label("Password"),"gap para")
//    contents+= (passField,"growx")}
}