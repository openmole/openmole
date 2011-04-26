/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.properties.IEnvironmentFactoryUI
import org.openmole.ide.core.workflow.model.IEntityUI

class EnvironmentUI(var name: String,val factory: IEnvironmentFactoryUI) extends IEntityUI{
  def this(factory: IEnvironmentFactoryUI)= this(MoleScenesManager.incrementEnvironmentName,factory)
}

//public class EnvironmentUI extends EntityUI{
//
//}