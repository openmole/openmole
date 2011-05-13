/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.workflow.model.IEntityUI

//class SamplingUI(var name: String, val coreClass: Class[_],val panel: PanelUI) extends IEntityUI {
//  def this(coreClass: Class[_],panel: PanelUI)= this(MoleScenesManager.incrementSamplingName,coreClass,panel)
//}
//
class SamplingUI(elementFactory: PaletteElementFactory) extends IEntityUI {
  override def factoryUI = elementFactory.factory
 // def this(coreClass: Class[_],panel: PanelUI)= this(MoleScenesManager.incrementSamplingName,coreClass,panel)
}