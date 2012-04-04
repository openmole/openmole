/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.dialog

import scala.swing._
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.core.implementation.panel.ConceptMenu
import java.awt.BorderLayout
import org.openmole.ide.core.implementation.control.TabManager
import org.openmole.ide.core.implementation.control.PasswordListner
import org.openide.DialogDescriptor
import org.openide.DialogDescriptor._
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openide.NotifyDescriptor._
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.preference.PreferenceContent
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.action.LoadXML
import org.openmole.ide.core.implementation.action.SaveXML
import org.openmole.ide.core.implementation.dataproxy.Proxys

class GUIPanel extends MainFrame {
  title = "OpenMOLE"
  
  menuBar = new MenuBar{ 
    contents += new Menu("File") {
      contents += new MenuItem(new Action("New Mole"){
          override def apply = DialogFactory.newTabName})
      
      contents += new MenuItem(new Action("Load"){
          override def apply = {
            Proxys.clearAll
            LoadXML.show
          }})
      
      contents += new MenuItem(new Action("Save"){
          override def apply = {
            TopComponentsManager.saveCurrentPropertyWidget
            SaveXML.save
          }})
      
      contents += new MenuItem(new Action("Save as"){
          override def apply = SaveXML.save(SaveXML.show)})
      
      contents += new MenuItem(new Action("Reset all"){
          override def apply = {
            TopComponentsManager.closeOpenedTopComponents
            Proxys.clearAll
          }})
    }
    
    contents += new Menu("Tools") {
      contents += new MenuItem(new Action("Preferences"){
          override def apply = {
            val pc = new PreferenceContent
            val dd = new DialogDescriptor(pc.peer, "Preferences")
            dd.setOptions(List(OK_OPTION).toArray)
            if (DialogDisplayer.getDefault.notify(dd).equals(OK_OPTION)) pc.save
          }})
    }
  }
  
  
  peer.setLayout(new BorderLayout)
  
  peer.add((new MigPanel("") {
        contents += ConceptMenu.prototypeMenu
        contents += ConceptMenu.taskMenu
        contents += ConceptMenu.samplingMenu
        contents += ConceptMenu.environmentMenu
      }).peer,BorderLayout.NORTH)

  peer.add((new TabManager).peer,BorderLayout.CENTER)
  
  peer.add((StatusBar).peer,BorderLayout.SOUTH)
  
  PasswordListner.apply
}
