/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import org.openmole.core.implementation.data.Prototype
import org.openmole.ide.core.workflow.implementation.PrototypeUI

trait IPrototypeFactoryUI  extends IFactoryUI {
  override def buildEntity(name: String,panel: PanelUI) = new PrototypeUI(name,coreClass, panel)
  
  override def buildEntity(panel: PanelUI) = new PrototypeUI(coreClass,panel)
  
  override def panel = null
  
  def coreObject(name: String): Prototype[_]
}

