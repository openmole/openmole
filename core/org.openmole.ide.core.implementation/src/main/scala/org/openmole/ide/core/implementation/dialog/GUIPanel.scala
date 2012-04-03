/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.dialog

import scala.swing.MainFrame
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.panel.ConceptMenu
import java.awt.BorderLayout
import org.openmole.ide.core.implementation.control.TabManager

class GUIPanel extends MainFrame {
  title = "OpenMOLE"
  peer.setLayout(new BorderLayout)
  
  peer.add((new MigPanel("") {
      contents += ConceptMenu.prototypeMenu
      contents += ConceptMenu.taskMenu
      contents += ConceptMenu.samplingMenu
      contents += ConceptMenu.environmentMenu
    }).peer,BorderLayout.NORTH)

  peer.add((new TabManager).peer,BorderLayout.CENTER)
  
  
  // menuBar = 
}
