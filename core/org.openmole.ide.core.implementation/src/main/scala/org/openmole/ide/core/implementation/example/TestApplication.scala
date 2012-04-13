/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.example

import java.awt.Frame
import org.openmole.ide.core.implementation.dialog.GUIApplication
import org.openmole.ide.core.implementation.dialog.SplashScreen
import scala.swing.SimpleGUIApplication

object TestApplication extends SimpleGUIApplication {
  def top = {
    val f = new SplashScreen
  //  f.peer.setExtendedState(Frame.MAXIMIZED_BOTH)
    f
  }
}
