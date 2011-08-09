/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.hook.display

import org.openmole.ide.core.model.data.IHookDataUI
import org.openmole.plugin.hook.display.DisplayHook

class DisplayHookDataUI(val name: String) extends IHookDataUI {
    
  override def coreClass= classOf[DisplayHook]
  
  override def imagePath = "img/thumb/hook.png"
  
  override def buildPanelUI = new DisplayHookPanelUI(this)
}
