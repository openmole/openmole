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
import javax.swing.JDialog
import javax.swing.JOptionPane
import scala.swing.Label
import scala.swing.PasswordField
import scala.swing.Window
import scala.swing.event.ButtonClicked
import org.openide.windows.WindowManager
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.misc.workspace.Workspace

object PasswordDialog extends Dialog{
  
  val passField = new PasswordField(12)
  val okButton = new Button("OK")
  val cancelButton = new Button("Cancel")
  
  contents= new MigPanel("") {
    contents+= new Label("Password: ")
    contents+= passField
    contents+= okButton
    contents+= cancelButton}
  
  listenTo(`okButton`,`cancelButton`) //login is a Button
  reactions += {
    case ButtonClicked(`okButton`) => 
        if (Workspace.passwordIsCorrect(passField.text)) {
          //Workspace.password= passField.text
          println("YES !!")
          visible= false}
    case ButtonClicked(`cancelButton`) => visible = false}
  
  visible= true
  
}
  
  
//  def show = {
//    val passField = new PasswordField
//    val result = new JOptionPane.showConfirmDialog(null,List(new Label("Password"),passField).toArray,"Password required",JOptionPane.OK_CANCEL_OPTION)
//   
//    if(result == JOptionPane.OK_OPTION){
//      if (Workspace.passwordIsCorrect) {println ("OK !!")}
//    }
//  }
//  
//     JLabel jUserName = new JLabel("User Name");
//        JTextField userName = new JTextField();
//        JLabel jPassword = new JLabel("Password");
//        JTextField password = new JPasswordField();
//        Object[] ob = {jUserName, userName, jPassword, password};
//        int result = JOptionPane.showConfirmDialog(null, ob, "Please input password for JOptionPane showConfirmDialog", JOptionPane.OK_CANCEL_OPTION);
// 
//        if (result == JOptionPane.OK_OPTION) {
//            String userNameValue = userName.getText();
//            String passwordValue = password.getText();
//            //Here is some validation code
//        }
  
  
// contents+= "aaaaaaaaaaaa"
//  new MigPanel("wrap 2") {
//    contents+= (new Label("Password"),"gap para")
//    contents+= (passField,"growx")}
//}