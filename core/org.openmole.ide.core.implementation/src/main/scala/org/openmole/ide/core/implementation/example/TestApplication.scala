/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.example

import org.openmole.ide.core.implementation.dialog.GUIApplication
import scala.swing.SimpleGUIApplication

object TestApplication extends SimpleGUIApplication {
  def top = (new GUIApplication).frame
}
