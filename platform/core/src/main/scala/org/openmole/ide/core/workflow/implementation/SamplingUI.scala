/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.ISamplingFactoryUI

//class SamplingUI(var name: String, val coreClass: Class[_],val panel: PanelUI) extends IEntityUI {
//  def this(coreClass: Class[_],panel: PanelUI)= this(MoleScenesManager.incrementSamplingName,coreClass,panel)
//}
//
class SamplingUI(factoryUI: IFactoryUI) extends EntityUI(factoryUI) {
 // def this(coreClass: Class[_],panel: PanelUI)= this(MoleScenesManager.incrementSamplingName,coreClass,panel)
}