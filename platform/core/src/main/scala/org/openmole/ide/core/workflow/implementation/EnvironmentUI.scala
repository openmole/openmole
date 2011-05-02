/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.properties.PanelUI
import org.openmole.ide.core.workflow.model.IEntityUI

class EnvironmentUI(var name: String,val coreClass: Class[_],val panel: PanelUI) extends IEntityUI{
  def this(coreClass: Class[_],panel: PanelUI)= this(MoleScenesManager.incrementEnvironmentName,coreClass,panel)
}

//public class EnvironmentUI extends EntityUI{
//
//}