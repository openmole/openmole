/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.PanelUI
import org.openmole.ide.core.properties.PanelUIData

trait IEntityUI {
  def factoryUI: IFactoryUI
  
  def panelUIData: PanelUIData
  
  def updatePanelUIData(pud: PanelUIData)
  
  
  //def coreObject = panel.coreObject(name)
  
  //def coreClass: Class[_]
}

//public interface IEntityUI {
//    String getName();
//    Class getType();
//}
