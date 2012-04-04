/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.example

import java.awt.Dimension
import org.openmole.ide.core.implementation.dialog.GUIPanel
import scala.swing.SimpleGUIApplication

object TestApplication extends SimpleGUIApplication {
  def top = new GUIPanel{preferredSize = new Dimension(400,200)}
}
