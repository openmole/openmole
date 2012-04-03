/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.dialog

import scala.swing.MainFrame
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.control.TabManager

class GUIPanel extends MainFrame {
  title = "OpenMOLE"
  contents = new PluginPanel("wrap") {
     contents += new TabManager
  }
  
  // menuBar = 
}
